//! 倒计时服务（对应 Java 版 `CountdownService`）。
//!
//! 与 UI 无关的状态机。Java 版用回调把事件推给 UI（需 invokeLater 切线程）；
//! Rust 版改为**事件通道 + 轮询**模型：egui 这类即时模式 UI 每帧调用
//! [`CountdownService::poll`] 拉取事件，天然规避跨线程回调问题。
//!
//! 事件契约与 Java 版一致：每秒收到 [`CountdownEvent::Tick`]（剩余秒数），
//! 结束时**先收到 `Tick(0)`（界面归零），再收到 [`CountdownEvent::Finished`]**。

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::mpsc::{self, Receiver, TryRecvError};
use std::thread::{self, JoinHandle};
use std::time::Duration;

/// 倒计时事件。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CountdownEvent {
    /// 每秒一次，携带剩余秒数；结束时固定为 `Tick(0)`
    Tick(u32),
    /// 倒计时结束（在 `Tick(0)` 之后发送）
    Finished,
}

pub struct CountdownService {
    rx: Option<Receiver<CountdownEvent>>,
    running: Option<std::sync::Arc<AtomicBool>>,
    // JoinHandle 被 drop 即 detach：worker 检测到取消标志后自行退出，无需 join
    worker: Option<JoinHandle<()>>,
}

impl CountdownService {
    pub fn new() -> Self {
        Self { rx: None, running: None, worker: None }
    }

    /// 开始倒计时；重复调用会先取消上一次。
    pub fn start(&mut self, seconds: u32) {
        self.cancel();
        let (tx, rx) = mpsc::channel();
        let running = std::sync::Arc::new(AtomicBool::new(true));
        let flag = std::sync::Arc::clone(&running);
        let worker = thread::Builder::new()
            .name("countdown-timer".into())
            .spawn(move || {
                // seconds==0 也要遵守契约：先 Tick(0) 再 Finished
                if seconds == 0 {
                    let _ = tx.send(CountdownEvent::Tick(0));
                    let _ = tx.send(CountdownEvent::Finished);
                    return;
                }
                let mut remaining = seconds;
                while remaining > 0 {
                    thread::sleep(Duration::from_secs(1));
                    if !flag.load(Ordering::Relaxed) {
                        return; // 已被新一次 start/cancel 取代
                    }
                    remaining -= 1;
                    if tx.send(CountdownEvent::Tick(remaining)).is_err() {
                        return; // 接收端已丢弃（服务被替换）
                    }
                }
                let _ = tx.send(CountdownEvent::Finished);
            })
            .ok();
        self.rx = Some(rx);
        self.running = Some(running);
        self.worker = worker;
    }

    /// 取消当前倒计时（不阻塞，worker 最迟 1 秒后退出）。
    pub fn cancel(&mut self) {
        if let Some(flag) = &self.running {
            flag.store(false, Ordering::Relaxed);
        }
        self.running = None;
        self.rx = None;
        self.worker = None;
    }

    /// 非阻塞地取一个事件；无事件时返回 None。
    pub fn poll(&mut self) -> Option<CountdownEvent> {
        match self.rx.as_ref()?.try_recv() {
            Ok(event) => Some(event),
            Err(TryRecvError::Empty) | Err(TryRecvError::Disconnected) => None,
        }
    }
}

impl Default for CountdownService {
    fn default() -> Self {
        Self::new()
    }
}

impl Drop for CountdownService {
    fn drop(&mut self) {
        // 通知 worker 退出，避免被丢弃后线程空跑至倒计时结束
        if let Some(flag) = &self.running {
            flag.store(false, Ordering::Relaxed);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::VecDeque;

    /// seconds=2 应依次产生 Tick(1)、Tick(0)、Finished，且 Tick(0) 在 Finished 之前
    #[test]
    fn ticks_down_to_zero_then_finishes() {
        let mut service = CountdownService::new();
        service.start(2);

        let mut events = VecDeque::new();
        let deadline = std::time::Instant::now() + Duration::from_secs(6);
        while events.len() < 3 && std::time::Instant::now() < deadline {
            if let Some(event) = service.poll() {
                events.push_back(event);
            } else {
                thread::sleep(Duration::from_millis(50));
            }
        }

        assert_eq!(events.pop_front(), Some(CountdownEvent::Tick(1)));
        assert_eq!(events.pop_front(), Some(CountdownEvent::Tick(0)));
        assert_eq!(events.pop_front(), Some(CountdownEvent::Finished));
    }

    /// 取消后不再有事件到达
    #[test]
    fn cancel_stops_events() {
        let mut service = CountdownService::new();
        service.start(60);
        thread::sleep(Duration::from_millis(1500)); // 等第一次 Tick
        assert_eq!(service.poll(), Some(CountdownEvent::Tick(59)));
        service.cancel();
        // worker 已被替换/停止：原通道被丢弃，poll 返回 None
        assert_eq!(service.poll(), None);
    }
}
