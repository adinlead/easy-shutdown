//! 内嵌资源：字体、音频、图标。
//!
//! 资源文件位于仓库 `src/main/resources/`，通过 `include_bytes!` 编译进二进制，
//! 分发时无需附带资源目录。

use std::io::Cursor;

/// 中文字体（文泉驿微米黑），egui 默认字体不含 CJK 字形，必须加载。
pub const FONT_WQY: &[u8] = include_bytes!("../../../resources/fonts/WQY-WeiMiHei.ttf");
/// LCD 风格数字字体，用于倒计时大字显示。
pub const FONT_NUMBER: &[u8] = include_bytes!("../../../resources/fonts/number-default.ttf");

/// 内置铃声（morning-joy.mp3）。
pub const DEFAULT_RING: &[u8] = include_bytes!("../../../resources/sounds/morning-joy.mp3");

/// 标题栏「托盘」按钮图标。
pub const ICON_BTN_TRAY: &[u8] = include_bytes!("../../../resources/img/act_btn/tray.png");
/// 标题栏「设置」按钮图标。
pub const ICON_BTN_SETTING: &[u8] = include_bytes!("../../../resources/img/act_btn/setting.png");
/// 标题栏「关闭」按钮图标。
pub const ICON_BTN_CLOSE: &[u8] = include_bytes!("../../../resources/img/act_btn/close.png");

/// 托盘图标（32x32 PNG）。
pub const ICON_TRAY_PNG: &[u8] = include_bytes!("../../../resources/img/logo/logo-32.png");
/// 应用 logo（64x64 PNG，标题栏显示）。
pub const ICON_LOGO_PNG: &[u8] = include_bytes!("../../../resources/img/logo/logo-64.png");

/// 将内嵌音频包装为可被 rodio 消费的 `Cursor<&[u8]>`。
pub fn default_ring_cursor() -> Cursor<&'static [u8]> {
    Cursor::new(DEFAULT_RING)
}

/// 从 PNG 字节解码为托盘图标。失败时返回空像素图标。
pub fn load_tray_icon() -> tray_icon::Icon {
    match image::load_from_memory(ICON_TRAY_PNG) {
        Ok(img) => {
            let rgba = img.to_rgba8();
            let (w, h) = rgba.dimensions();
            tray_icon::Icon::from_rgba(rgba.into_raw(), w, h).unwrap_or_else(|_| empty_icon())
        }
        Err(_) => empty_icon(),
    }
}

/// 把 PNG 原样解码为 egui 窗口图标（任务栏、Alt-Tab 使用）。
///
/// 与 exe 文件图标是两回事：文件图标来自 PE 资源段（见 build.rs），
/// 而运行中窗口的任务栏图标来自 `ViewportBuilder::with_icon`。
pub fn load_window_icon(png: &[u8]) -> Option<egui::IconData> {
    let rgba = image::load_from_memory(png).ok()?.to_rgba8();
    let (width, height) = rgba.dimensions();
    Some(egui::IconData {
        rgba: rgba.into_raw(),
        width,
        height,
    })
}

/// 把 PNG 原样解码为 egui 纹理（用于彩色 logo）。
pub fn load_color_texture(
    ctx: &egui::Context,
    name: &str,
    png: &[u8],
) -> egui::TextureHandle {
    let (pixels, size) = match image::load_from_memory(png) {
        Ok(img) => {
            let rgba = img.to_rgba8();
            let (w, h) = rgba.dimensions();
            let pixels = rgba
                .pixels()
                .map(|p| egui::Color32::from_rgba_unmultiplied(p[0], p[1], p[2], p[3]))
                .collect::<Vec<_>>();
            (pixels, [w as usize, h as usize])
        }
        Err(_) => (vec![egui::Color32::TRANSPARENT], [1, 1]),
    };
    ctx.load_texture(name, egui::ColorImage { size, pixels }, egui::TextureOptions::LINEAR)
}

/// 把 PNG 按 `tint` 重新着色后载入 egui 纹理。
///
/// 仓库中的标题栏图标都是「透明底 + 纯黑线条」的 alpha 蒙版，
/// 深色主题下黑色线条几乎不可见，因此这里丢弃原 RGB、只用 alpha 作为形状，
/// 用 `tint` 作为线条颜色重新着色，保证与背景有足够对比度。
pub fn load_tinted_texture(
    ctx: &egui::Context,
    name: &str,
    png: &[u8],
    tint: egui::Color32,
) -> egui::TextureHandle {
    let (pixels, size) = match image::load_from_memory(png) {
        Ok(img) => {
            let rgba = img.to_rgba8();
            let (w, h) = rgba.dimensions();
            let pixels = rgba
                .pixels()
                .map(|p| {
                    if p[3] == 0 {
                        egui::Color32::TRANSPARENT
                    } else {
                        egui::Color32::from_rgba_unmultiplied(tint.r(), tint.g(), tint.b(), p[3])
                    }
                })
                .collect::<Vec<_>>();
            (pixels, [w as usize, h as usize])
        }
        Err(_) => (vec![egui::Color32::TRANSPARENT], [1, 1]),
    };
    ctx.load_texture(name, egui::ColorImage { size, pixels }, egui::TextureOptions::LINEAR)
}

fn empty_icon() -> tray_icon::Icon {
    tray_icon::Icon::from_rgba(vec![0u8; 4], 1, 1).unwrap()
}
