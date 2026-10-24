//! MP3 播放服务（对应 Java 版 `AudioService`，jlayer → rodio）。
//!
//! 同一时间只播放一个音频，重复调用 [`AudioService::play`] 会先停止上一个。
//! 播放由 rodio 的后台音频线程驱动，不阻塞调用方。

use std::io::{Read, Seek};

use rodio::{Decoder, OutputStream, OutputStreamHandle, Sink};

use crate::core::error::OperationError;

pub struct AudioService {
    // OutputStream 必须保活，drop 会关闭音频输出
    _stream: Option<OutputStream>,
    handle: Option<OutputStreamHandle>,
    sink: Option<Sink>,
}

impl AudioService {
    /// 初始化音频输出设备。无可用输出设备时返回错误（UI 边界负责提示）。
    pub fn new() -> Result<Self, OperationError> {
        let (stream, handle) = OutputStream::try_default()
            .map_err(|e| OperationError::with_source("音频输出设备初始化失败", e))?;
        Ok(Self { _stream: Some(stream), handle: Some(handle), sink: None })
    }

    /// 播放 MP3 数据。`reader` 需要可读且可定位（`Cursor<Vec<u8>>` 内嵌资源
    /// 或 `File` 磁盘文件均可）。
    pub fn play<R>(&mut self, reader: R) -> Result<(), OperationError>
    where
        R: Read + Seek + Send + Sync + 'static,
    {
        self.stop();
        let source = Decoder::new_mp3(reader)
            .map_err(|e| OperationError::with_source("MP3 解码失败", e))?;
        let handle = self
            .handle
            .as_ref()
            .ok_or_else(|| OperationError::new("音频服务未初始化"))?;
        let sink = Sink::try_new(handle)
            .map_err(|e| OperationError::with_source("音频通道创建失败", e))?;
        sink.append(source);
        // 不调用 detach（按值消费 sink）：保留在 self.sink 中以便 stop() 控制
        self.sink = Some(sink);
        Ok(())
    }

    /// 停止当前播放。
    pub fn stop(&mut self) {
        if let Some(sink) = self.sink.take() {
            sink.stop();
        }
    }
}
