//! EasyShutdown（Rust 版）入口：创建系统托盘 + 启动 eframe 窗口。
//!
//! release 构建声明 windows 子系统，避免双击时弹出控制台黑窗口；
//! debug 构建保留控制台，便于查看日志和 panic 信息。
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use tray_icon::menu::{Menu, MenuEvent, MenuItem};
use tray_icon::{TrayIconBuilder, TrayIconEvent};

use easy_shutdown::core::config::AppConfig;
use easy_shutdown::ui::app::{strings_for_lang, App, UiEvent};
use easy_shutdown::ui::resources::{load_tray_icon, load_window_icon, ICON_LOGO_PNG};

/// 取原生窗口句柄（Windows），供托盘菜单线程直接投递 `WM_CLOSE`。
#[cfg(target_os = "windows")]
fn native_window_handle(cc: &eframe::CreationContext<'_>) -> Option<isize> {
    use raw_window_handle::{HasWindowHandle, RawWindowHandle};
    match cc.window_handle().ok()?.as_raw() {
        RawWindowHandle::Win32(handle) => Some(handle.hwnd.get()),
        _ => None,
    }
}

fn main() -> Result<(), eframe::Error> {
    let config = AppConfig::load();
    let strings = strings_for_lang(config.lang());

    // ---- 系统托盘 ----
    let menu = Menu::new();
    let show_item = MenuItem::new(strings.tray_show, true, None);
    let quit_item = MenuItem::new(strings.tray_exit, true, None);
    menu.append(&show_item).expect("append show");
    menu.append(&quit_item).expect("append quit");

    let tray = TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_icon(load_tray_icon())
        .with_tooltip(strings.title)
        .build()
        .expect("build tray icon");

    let tray_icon_rx = TrayIconEvent::receiver().clone();
    let menu_rx = MenuEvent::receiver().clone();

    // 托盘事件由 OS 投递到后台线程，而 egui 只在有输入/窗口事件时重绘，
    // 直接 try_recv 会导致「点托盘菜单后界面卡住，直到鼠标划过窗口才响应」。
    // 这里用转发线程：把事件送进 UI 通道后主动唤醒 egui。
    let (ui_tx, ui_rx) = crossbeam_channel::unbounded::<UiEvent>();

    // ---- eframe 窗口 ----
    let mut viewport = egui::ViewportBuilder::default()
        .with_decorations(false) // 无边框，自绘标题栏 + 拖拽
        .with_inner_size([420.0, 320.0])
        .with_resizable(false)
        .with_title(strings.title);
    // 任务栏/Alt-Tab 图标：exe 资源图标管不到运行中的窗口，必须显式设置
    if let Some(icon) = load_window_icon(ICON_LOGO_PNG) {
        viewport = viewport.with_icon(icon);
    }
    let options = eframe::NativeOptions {
        viewport,
        ..Default::default()
    };

    let show_id = show_item.id().clone();
    let quit_id = quit_item.id().clone();

    eframe::run_native(
        strings.title,
        options,
        Box::new(move |cc| {
            // 字体与主题必须在首帧渲染前安装，否则自定义字族尚未注册会 panic
            App::install(&cc.egui_ctx);

            #[cfg(target_os = "windows")]
            let window_handle = native_window_handle(cc);

            let ctx = cc.egui_ctx.clone();
            let tx = ui_tx.clone();
            std::thread::spawn(move || {
                for event in tray_icon_rx {
                    if tx.send(UiEvent::Tray(event)).is_err() {
                        break;
                    }
                    ctx.request_repaint();
                }
            });

            // 菜单线程：菜单项的事件在 winit 主循环忙于弹出菜单时到达，
            // 若先转发给 UI 线程再等一帧处理，会出现「点了退出但界面纹丝不动」。
            // 因此退出项在这里就地处理，不依赖 UI 线程。
            let ctx = cc.egui_ctx.clone();
            let tx = ui_tx.clone();
            let thread_quit_id = quit_id.clone();
            std::thread::spawn(move || {
                for event in menu_rx {
                    if thread_quit_id == event.id() {
                        // 托盘菜单弹出时，主线程阻塞在 TrackPopupMenu 的模态消息循环里，
                        // 期间 egui 的 request_repaint 可能被当成过期请求丢弃，
                        // 表现为「点了退出没反应，直到鼠标划过窗口才退出」。
                        // 这里直接向窗口投递 WM_CLOSE，由系统消息队列唤醒主线程。
                        #[cfg(target_os = "windows")]
                        if let Some(hwnd) = window_handle {
                            use windows::Win32::Foundation::{HWND, LPARAM, WPARAM};
                            use windows::Win32::UI::WindowsAndMessaging::{PostMessageW, WM_CLOSE};
                            unsafe {
                                let _ = PostMessageW(
                                    HWND(hwnd as *mut core::ffi::c_void),
                                    WM_CLOSE,
                                    WPARAM(0),
                                    LPARAM(0),
                                );
                            }
                        }
                        ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                        ctx.request_repaint();
                        break;
                    }
                    if tx.send(UiEvent::Menu(event)).is_err() {
                        break;
                    }
                    ctx.request_repaint();
                }
            });

            Ok(Box::new(App::new(
                &cc.egui_ctx,
                config,
                ui_rx,
                show_id,
                quit_id,
            )))
        }),
    )?;

    // 显式移除托盘图标后再让进程结束。
    // 直接 process::exit 会跳过 TrayIcon 的 Drop，Windows 托盘会残留"幽灵图标"。
    let _ = tray.set_visible(false);
    drop(tray);

    Ok(())
}
