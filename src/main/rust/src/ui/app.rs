//! eframe 主应用：主窗口、倒计时、操作按钮、设置弹窗、托盘集成。

use egui::{Color32, FontId, RichText, Sense, Stroke, Vec2};

use crate::core::audio::AudioService;
use crate::core::config::{AppConfig, LANG_SYSTEM};
use crate::core::countdown::{CountdownEvent, CountdownService};
use crate::core::operator;
use crate::ui::resources::{self, default_ring_cursor};
use crate::ui::time_scale::{format_duration, slider_to_minutes, SLIDER_MAX};

/// 深色主题配色。
///
/// 刻意采用「深底 + 近白字」的高对比组合：
/// 主文字与背景对比度约 16:1，次要文字约 7:1，均高于 WCAG AA 要求的 4.5:1。
mod theme {
    use egui::Color32;

    /// 窗口主体背景
    pub const BG: Color32 = Color32::from_rgb(0x0F, 0x12, 0x18);
    /// 标题栏背景（比主体略亮，形成层次）
    pub const BAR: Color32 = Color32::from_rgb(0x19, 0x1F, 0x29);
    /// 控件常态背景
    pub const CARD: Color32 = Color32::from_rgb(0x23, 0x2A, 0x38);
    /// 悬停背景
    pub const HOVER: Color32 = Color32::from_rgb(0x30, 0x3A, 0x4C);
    /// 边框
    pub const BORDER: Color32 = Color32::from_rgb(0x3C, 0x48, 0x5C);
    /// 主文字（近白）
    pub const TEXT: Color32 = Color32::from_rgb(0xF3, 0xF6, 0xFB);
    /// 次要文字
    pub const DIM: Color32 = Color32::from_rgb(0xA3, 0xB0, 0xC4);
    /// 强调色（选中态、主按钮）
    pub const ACCENT: Color32 = Color32::from_rgb(0x2F, 0x7D, 0xF6);
    /// 强调色悬停态
    pub const ACCENT_HOVER: Color32 = Color32::from_rgb(0x4A, 0x91, 0xFA);
    /// LCD 倒计时（运行中）
    pub const LCD: Color32 = Color32::from_rgb(0x4F, 0xE3, 0x8A);
    /// LCD 倒计时（未启动）
    pub const LCD_IDLE: Color32 = Color32::from_rgb(0x8C, 0x9A, 0xB0);
    /// 危险/取消按钮
    pub const DANGER: Color32 = Color32::from_rgb(0xE0, 0x4B, 0x4B);
    /// 危险按钮悬停态
    pub const DANGER_HOVER: Color32 = Color32::from_rgb(0xEF, 0x5F, 0x5F);
    /// 图标线条颜色
    pub const ICON: Color32 = Color32::from_rgb(0xCF, 0xD9, 0xE8);
}

/// 用户可执行的定时动作。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Operation {
    Shutdown,
    Reboot,
    Ring,
}

/// 界面文案（中文/英文），与 `resources/locale/*.json` 保持同源。
#[derive(Clone)]
pub struct Strings {
    pub title: &'static str,
    pub tray: &'static str,
    pub setting: &'static str,
    pub close: &'static str,
    /// 滑块刻度标签，对应滑块值 0/30/60/84/102/126/150。
    pub ticks: [&'static str; 7],
    pub shutdown: &'static str,
    pub restart: &'static str,
    pub ringing: &'static str,
    pub start: &'static str,
    pub cancel: &'static str,
    pub idle: &'static str,
    pub remaining: &'static str,
    pub ringing_title: &'static str,
    pub stop_ring: &'static str,
    pub settings_title: &'static str,
    pub settings_lang: &'static str,
    pub settings_audio: &'static str,
    pub settings_browse: &'static str,
    pub settings_reset: &'static str,
    pub settings_default_audio: &'static str,
    pub save: &'static str,
    pub lang_system: &'static str,
    pub lang_zh: &'static str,
    pub lang_en: &'static str,
    pub tray_show: &'static str,
    pub tray_exit: &'static str,
    pub error_audio: &'static str,
    pub error_shutdown: &'static str,
    pub error_config: &'static str,
}

impl Strings {
    pub fn zh() -> Self {
        Self {
            title: "易关机",
            tray: "托盘",
            setting: "设置",
            close: "关闭",
            ticks: ["现在", "30分钟", "1小时", "3小时", "6小时", "12小时", "24小时"],
            shutdown: "关机",
            restart: "重启",
            ringing: "响铃",
            start: "开始",
            cancel: "取消",
            idle: "未启动",
            remaining: "剩余",
            ringing_title: "响铃中",
            stop_ring: "停止响铃",
            settings_title: "设置",
            settings_lang: "语言",
            settings_audio: "铃声文件",
            settings_browse: "浏览",
            settings_reset: "恢复默认",
            settings_default_audio: "（内置铃声）",
            save: "确定",
            lang_system: "跟随系统",
            lang_zh: "简体中文",
            lang_en: "English",
            tray_show: "显示窗口",
            tray_exit: "退出",
            error_audio: "铃声播放失败",
            error_shutdown: "执行失败",
            error_config: "保存配置失败",
        }
    }

    pub fn en() -> Self {
        Self {
            title: "Easy Shutdown",
            tray: "Tray",
            setting: "Setting",
            close: "Close",
            ticks: ["NOW", "30MN", "1H", "3H", "6H", "12H", "24H"],
            shutdown: "Shutdown",
            restart: "Reboot",
            ringing: "Ringing",
            start: "Start",
            cancel: "Cancel",
            idle: "Idle",
            remaining: "Remaining",
            ringing_title: "Ringing",
            stop_ring: "Stop",
            settings_title: "Settings",
            settings_lang: "Language",
            settings_audio: "Ring file",
            settings_browse: "Browse",
            settings_reset: "Reset",
            settings_default_audio: "(built-in ring)",
            save: "OK",
            lang_system: "System",
            lang_zh: "简体中文",
            lang_en: "English",
            tray_show: "Show window",
            tray_exit: "Exit",
            error_audio: "Failed to play ringtone",
            error_shutdown: "Operation failed",
            error_config: "Failed to save config",
        }
    }
}

/// 托盘线程转发给 UI 线程的事件。
///
/// 托盘事件由操作系统在其他线程投递，而 egui 只在有输入/窗口事件时重绘，
/// 所以必须转发到 UI 线程并主动唤醒 egui，否则响应会被拖到下次鼠标移动。
pub enum UiEvent {
    Tray(tray_icon::TrayIconEvent),
    Menu(tray_icon::menu::MenuEvent),
}

/// 应用运行时状态。
pub struct App {
    countdown: CountdownService,
    audio: Option<AudioService>,
    config: AppConfig,
    strings: Strings,
    /// 待显示的错误消息。
    error: Option<String>,

    // ---- 图标纹理 ----
    tex_logo: egui::TextureHandle,
    tex_tray: egui::TextureHandle,
    tex_setting: egui::TextureHandle,
    tex_close: egui::TextureHandle,

    // ---- 倒计时 ----
    op: Operation,
    slider: u32,
    /// `None` 表示未启动。
    remaining: Option<u32>,
    running: bool,
    /// 是否正在响铃（决定是否弹出停止框）。
    ringing: bool,

    // ---- 设置弹窗 ----
    settings_open: bool,
    pending_lang: String,
    pending_audio: String,

    // ---- 托盘 ----
    ui_events: crossbeam_channel::Receiver<UiEvent>,
    menu_show_id: tray_icon::menu::MenuId,
    menu_quit_id: tray_icon::menu::MenuId,

    window_visible: bool,
}

impl App {
    /// 一次性安装字体与主题样式。
    ///
    /// 必须在首帧渲染之前调用（`run_native` 的创建回调中）：
    /// `set_fonts` / `set_visuals` 对当前帧不立即生效，若在 `update` 里调用，
    /// 首帧仍使用默认字族与浅色主题，引用自定义字族会直接 panic。
    pub fn install(ctx: &egui::Context) {
        // ---------- 字体 ----------
        let mut fonts = egui::FontDefinitions::default();
        fonts.font_data.insert(
            "wqy".into(),
            egui::FontData::from_static(resources::FONT_WQY),
        );
        fonts.font_data.insert(
            "lcd".into(),
            egui::FontData::from_static(resources::FONT_NUMBER),
        );
        // CJK 字体放在西文默认字体之前，保证中文优先命中
        fonts
            .families
            .entry(egui::FontFamily::Proportional)
            .or_default()
            .insert(0, "wqy".into());
        fonts
            .families
            .entry(egui::FontFamily::Monospace)
            .or_default()
            .push("wqy".into());
        // LCD 数字字体单独分组；追加 wqy 作为兜底，
        // 避免数字字体缺少 `:` 等字符时渲染成方块
        fonts.families.insert(
            egui::FontFamily::Name("lcd".into()),
            vec!["lcd".into(), "wqy".into()],
        );
        ctx.set_fonts(fonts);

        // ---------- 主题 ----------
        let mut v = egui::Visuals::dark();
        v.panel_fill = theme::BG;
        v.window_fill = theme::BAR;
        v.window_stroke = Stroke::new(1.0_f32, theme::BORDER);
        v.extreme_bg_color = theme::CARD;
        v.faint_bg_color = theme::CARD;
        v.override_text_color = Some(theme::TEXT);
        v.selection.bg_fill = theme::ACCENT;
        v.selection.stroke = Stroke::new(1.0_f32, Color32::WHITE);

        v.widgets.noninteractive.bg_fill = theme::CARD;
        v.widgets.noninteractive.weak_bg_fill = theme::CARD;
        v.widgets.noninteractive.bg_stroke = Stroke::new(1.0_f32, theme::BORDER);
        v.widgets.noninteractive.fg_stroke = Stroke::new(1.0_f32, theme::TEXT);

        v.widgets.inactive.bg_fill = theme::CARD;
        v.widgets.inactive.weak_bg_fill = theme::CARD;
        v.widgets.inactive.bg_stroke = Stroke::new(1.0_f32, theme::BORDER);
        v.widgets.inactive.fg_stroke = Stroke::new(1.0_f32, theme::TEXT);

        v.widgets.hovered.bg_fill = theme::HOVER;
        v.widgets.hovered.weak_bg_fill = theme::HOVER;
        v.widgets.hovered.bg_stroke = Stroke::new(1.0_f32, theme::ACCENT);
        v.widgets.hovered.fg_stroke = Stroke::new(1.5_f32, theme::TEXT);

        v.widgets.active.bg_fill = theme::ACCENT;
        v.widgets.active.weak_bg_fill = theme::ACCENT;
        v.widgets.active.bg_stroke = Stroke::new(1.0_f32, theme::ACCENT);
        v.widgets.active.fg_stroke = Stroke::new(1.5_f32, Color32::WHITE);

        // 深色主题必须写入「两套」样式：
        // egui 按明/暗两套 Style 分别存储，`set_visuals` 只改当前主题那一套，
        // 而 egui-winit 会在首帧根据系统外观切换主题（系统为浅色时切到 Light），
        // 若只改当前主题，切换后会退回未修改的浅色样式（弹窗变成灰白）。
        ctx.all_styles_mut(|style| {
            style.visuals = v.clone();
            style.spacing.item_spacing = Vec2::new(8.0, 8.0);
            style.spacing.button_padding = Vec2::new(12.0, 7.0);
        });
        // 强制使用深色外观，不跟随系统
        ctx.set_theme(egui::Theme::Dark);
    }

    pub fn new(
        ctx: &egui::Context,
        config: AppConfig,
        ui_events: crossbeam_channel::Receiver<UiEvent>,
        menu_show_id: tray_icon::menu::MenuId,
        menu_quit_id: tray_icon::menu::MenuId,
    ) -> Self {
        let strings = strings_for_lang(config.lang());
        Self {
            countdown: CountdownService::new(),
            audio: None,
            config,
            strings,
            error: None,
            tex_logo: resources::load_color_texture(ctx, "logo", resources::ICON_LOGO_PNG),
            tex_tray: resources::load_tinted_texture(
                ctx,
                "icon-tray",
                resources::ICON_BTN_TRAY,
                theme::ICON,
            ),
            tex_setting: resources::load_tinted_texture(
                ctx,
                "icon-setting",
                resources::ICON_BTN_SETTING,
                theme::ICON,
            ),
            tex_close: resources::load_tinted_texture(
                ctx,
                "icon-close",
                resources::ICON_BTN_CLOSE,
                theme::ICON,
            ),
            op: Operation::Shutdown,
            slider: 30,
            remaining: None,
            running: false,
            ringing: false,
            settings_open: false,
            pending_lang: String::new(),
            pending_audio: String::new(),
            ui_events,
            menu_show_id,
            menu_quit_id,
            window_visible: true,
        }
    }

    fn apply_lang(&mut self, lang: &str) {
        self.strings = strings_for_lang(lang);
    }

    /// 当前滑块对应的时长（秒）。
    fn selected_seconds(&self) -> u32 {
        slider_to_minutes(self.slider) * 60
    }

    fn start_countdown(&mut self) {
        self.error = None;
        // 重新开始倒计时时先停掉上一次的铃声
        if self.ringing {
            self.stop_ring();
        }
        let seconds = self.selected_seconds();
        if seconds == 0 {
            self.execute_operation();
            return;
        }
        self.remaining = Some(seconds);
        self.running = true;
        self.countdown.start(seconds);
    }

    fn cancel_countdown(&mut self) {
        self.countdown.cancel();
        self.running = false;
        self.remaining = None;
    }

    fn execute_operation(&mut self) {
        self.running = false;
        self.remaining = None;
        let result = match self.op {
            Operation::Shutdown => operator::shutdown(),
            Operation::Reboot => operator::reboot(),
            Operation::Ring => self.play_ring(),
        };
        if let Err(e) = result {
            let msg = match self.op {
                Operation::Ring => self.strings.error_audio,
                _ => self.strings.error_shutdown,
            };
            self.error = Some(format!("{msg}: {e}"));
        }
    }

    fn play_ring(&mut self) -> Result<(), crate::core::error::OperationError> {
        let audio_file = self.config.audio_file().to_string();
        // 取出再放回，避免同时借用 self.audio 与 self 的其他字段
        let mut audio = match self.audio.take() {
            Some(a) => a,
            None => AudioService::new()?,
        };
        let result = if audio_file.is_empty() || audio_file == "#default_01" {
            audio.play(default_ring_cursor())
        } else {
            match std::fs::File::open(&audio_file) {
                Ok(file) => audio.play(file),
                Err(e) => Err(crate::core::error::OperationError::with_source(
                    "open audio file",
                    e,
                )),
            }
        };
        self.audio = Some(audio);
        // 播放成功才弹出停止框
        if result.is_ok() {
            self.ringing = true;
        }
        result
    }

    /// 停止响铃并关闭停止框。
    fn stop_ring(&mut self) {
        if let Some(audio) = self.audio.as_mut() {
            audio.stop();
        }
        self.ringing = false;
    }

    fn open_settings(&mut self) {
        self.pending_lang = self.config.lang().to_string();
        self.pending_audio = self.config.audio_file().to_string();
        self.settings_open = true;
    }

    fn save_settings(&mut self) {
        if self.pending_lang != self.config.lang() {
            self.config.set_lang(self.pending_lang.clone());
            let lang = self.config.lang().to_string();
            self.apply_lang(&lang);
        }
        if self.pending_audio != self.config.audio_file() {
            self.config.set_audio_file(self.pending_audio.clone());
        }
        if let Err(e) = self.config.save() {
            self.error = Some(format!("{}: {e}", self.strings.error_config));
        }
        self.settings_open = false;
    }

    fn hide_to_tray(&mut self, ctx: &egui::Context) {
        self.window_visible = false;
        ctx.send_viewport_cmd(egui::ViewportCommand::Visible(false));
    }

    fn quit(&mut self, ctx: &egui::Context) {
        // 只通知 eframe 关闭视口；托盘图标由主线程在 run_native 返回后清理，
        // 不能在这里直接 process::exit —— 那样 TrayIcon 的 Drop 不会执行，
        // Windows 会残留一个"幽灵图标"。
        ctx.send_viewport_cmd(egui::ViewportCommand::Close);
        ctx.request_repaint();
    }

    /// 处理托盘事件。
    fn handle_tray(&mut self, ctx: &egui::Context) {
        while let Ok(event) = self.ui_events.try_recv() {
            match event {
                // 只响应左键单击：右键是"打开菜单"，若也切换可见性会误隐藏窗口
                UiEvent::Tray(tray_event) => {
                    if tray_event.click_type == tray_icon::ClickType::Left {
                        self.window_visible = !self.window_visible;
                        ctx.send_viewport_cmd(egui::ViewportCommand::Visible(self.window_visible));
                    }
                }
                UiEvent::Menu(menu_event) => {
                    if self.menu_show_id == menu_event.id() {
                        self.window_visible = true;
                        ctx.send_viewport_cmd(egui::ViewportCommand::Visible(true));
                    } else if self.menu_quit_id == menu_event.id() {
                        self.quit(ctx);
                    }
                }
            }
        }
    }
}

impl eframe::App for App {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        self.handle_tray(ctx);

        // 轮询倒计时事件
        while let Some(event) = self.countdown.poll() {
            match event {
                CountdownEvent::Tick(s) => self.remaining = Some(s),
                CountdownEvent::Finished => self.execute_operation(),
            }
        }

        if self.running {
            ctx.request_repaint_after(std::time::Duration::from_millis(200));
        }

        egui::TopBottomPanel::top("title_bar")
            .frame(
                egui::Frame::none()
                    .fill(theme::BAR)
                    .inner_margin(egui::Margin::symmetric(10.0, 5.0)),
            )
            .show(ctx, |ui| {
                self.title_bar(ui);
            });

        egui::CentralPanel::default()
            .frame(
                egui::Frame::none()
                    .fill(theme::BG)
                    .inner_margin(egui::Margin::symmetric(18.0, 14.0)),
            )
            .show(ctx, |ui| {
                self.main_content(ui);
            });

        if self.settings_open {
            self.settings_window(ctx);
        }

        if self.ringing {
            self.ring_dialog(ctx);
        }
    }
}

impl App {
    /// 自绘标题栏：logo + 标题（可拖拽），右侧托盘/设置/关闭图标按钮。
    fn title_bar(&mut self, ui: &mut egui::Ui) {
        let bar_h = 32.0;
        let btn_w = 30.0;
        let title = self.strings.title;
        let tray_tip = self.strings.tray;
        let setting_tip = self.strings.setting;
        let close_tip = self.strings.close;

        // 先按右对齐布局放置三个图标按钮，再把剩余宽度作为拖拽区
        let mut do_hide = false;
        let mut do_settings = false;
        let mut do_quit = false;

        let tex_tray = self.tex_tray.clone();
        let tex_setting = self.tex_setting.clone();
        let tex_close = self.tex_close.clone();

        ui.horizontal(|ui| {
            ui.set_height(bar_h);
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui.spacing_mut().item_spacing.x = 4.0;
                do_quit = icon_button(ui, &tex_close, close_tip).clicked();
                do_settings = icon_button(ui, &tex_setting, setting_tip).clicked();
                do_hide = icon_button(ui, &tex_tray, tray_tip).clicked();
            });
        });

        // 拖拽区覆盖整条标题栏（图标按钮由 egui 的交互优先级覆盖）
        let full = ui.max_rect();
        let drag_rect = egui::Rect::from_min_max(
            full.left_top(),
            egui::pos2(full.right() - btn_w * 3.0 - 12.0, full.top() + bar_h + 10.0),
        )
        .intersect(full);
        let resp = ui.interact(
            drag_rect,
            ui.id().with("title_drag"),
            Sense::click_and_drag(),
        );
        if resp.hovered() && ui.input(|i| i.pointer.primary_pressed()) {
            ui.ctx().send_viewport_cmd(egui::ViewportCommand::StartDrag);
        }
        if resp.dragged() {
            ui.ctx().send_viewport_cmd(egui::ViewportCommand::StartDrag);
        }

        // logo + 标题
        let logo_size = 22.0;
        let logo_rect = egui::Rect::from_center_size(
            egui::pos2(drag_rect.left() + logo_size / 2.0, drag_rect.center().y),
            Vec2::splat(logo_size),
        );
        egui::Image::new(egui::load::SizedTexture::from_handle(&self.tex_logo))
            .paint_at(ui, logo_rect);
        ui.painter().text(
            egui::pos2(logo_rect.right() + 8.0, logo_rect.center().y),
            egui::Align2::LEFT_CENTER,
            title,
            FontId::proportional(15.0),
            theme::TEXT,
        );

        if do_hide {
            self.hide_to_tray(ui.ctx());
        }
        if do_settings {
            self.open_settings();
        }
        if do_quit {
            self.quit(ui.ctx());
        }
    }

    fn main_content(&mut self, ui: &mut egui::Ui) {
        let s = self.strings.clone();
        let avail_w = ui.available_width();

        // ---------- 倒计时大字 ----------
        // 未启动时显示滑块对应的目标时长（同样使用 LCD 字体）
        let (display, color) = match self.remaining {
            Some(secs) => (format_duration(secs), theme::LCD),
            None => (format_duration(self.selected_seconds()), theme::LCD_IDLE),
        };
        let status = if self.running { s.remaining } else { s.idle };

        ui.add_space(6.0);
        ui.vertical_centered(|ui| {
            ui.label(
                RichText::new(display)
                    .font(FontId::new(56.0, egui::FontFamily::Name("lcd".into())))
                    .color(color),
            );
            ui.add_space(2.0);
            ui.label(RichText::new(status).size(12.0).color(theme::DIM));
        });

        // ---------- 时间滑块 + 刻度 ----------
        ui.add_space(16.0);
        ui.spacing_mut().slider_width = (avail_w - 20.0).max(60.0);
        let slider_resp = ui.add(
            egui::Slider::new(&mut self.slider, 0..=SLIDER_MAX)
                .show_value(false)
                .handle_shape(egui::style::HandleShape::Circle),
        );

        // 鼠标悬停在滑块上时用滚轮微调（egui 0.29 的 Slider 本身不处理滚轮）。
        // 一格滚轮按 40 点约等于一个刻度；滚轮放大时按比例多走几步。
        if slider_resp.hovered() {
            let scroll = ui.input(|i| i.raw_scroll_delta.y);
            if scroll != 0.0 {
                let steps = (scroll.abs() / 40.0).round().max(1.0) as i64;
                let delta = if scroll > 0.0 { steps } else { -steps };
                let next = (self.slider as i64 + delta).clamp(0, SLIDER_MAX as i64);
                self.slider = next as u32;
            }
        }

        draw_ticks(ui, &slider_resp, self.slider, &s.ticks);

        // ---------- 底部：左侧操作单选框 + 右侧开始/取消 ----------
        ui.add_space(16.0);
        let row_h = 68.0;
        let mut op = self.op;
        let mut clicked = false;
        ui.horizontal(|ui| {
            ui.spacing_mut().item_spacing.x = 14.0;

            // 三个操作单选项纵向排列，与右侧按钮等高
            ui.vertical(|ui| {
                ui.spacing_mut().item_spacing.y = 4.0;
                ui.add_enabled_ui(!self.running, |ui| {
                    ui.radio_value(&mut op, Operation::Shutdown, s.shutdown);
                    ui.radio_value(&mut op, Operation::Reboot, s.restart);
                    ui.radio_value(&mut op, Operation::Ring, s.ringing);
                });
            });

            let (label, fill, hover) = if self.running {
                (s.cancel, theme::DANGER, theme::DANGER_HOVER)
            } else {
                (s.start, theme::ACCENT, theme::ACCENT_HOVER)
            };
            clicked = big_button(ui, label, fill, hover, ui.available_width(), row_h).clicked();
        });
        if op != self.op {
            self.op = op;
        }
        if clicked {
            if self.running {
                self.cancel_countdown();
            } else {
                self.start_countdown();
            }
        }

        // ---------- 错误提示 ----------
        if let Some(err) = &self.error {
            ui.add_space(10.0);
            ui.label(RichText::new(err).color(theme::DANGER).size(12.0));
        }
    }

    /// 响铃停止框：铃声播放期间居中显示，只有「停止响铃」能关闭。
    fn ring_dialog(&mut self, ctx: &egui::Context) {
        let s = self.strings.clone();
        let mut stop = false;
        egui::Window::new(s.ringing_title)
            .collapsible(false)
            .resizable(false)
            .anchor(egui::Align2::CENTER_CENTER, Vec2::ZERO)
            .show(ctx, |ui| {
                ui.set_width(200.0);
                ui.add_space(4.0);
                stop = big_button(
                    ui,
                    s.stop_ring,
                    theme::DANGER,
                    theme::DANGER_HOVER,
                    ui.available_width(),
                    40.0,
                )
                .clicked();
            });
        if stop {
            self.stop_ring();
        }
    }

    fn settings_window(&mut self, ctx: &egui::Context) {
        let s = self.strings.clone();
        // `open` 由窗口标题栏的叉号控制；按钮的意图单独记录，
        // 否则窗口渲染结束后用 `open` 回写状态会把按钮的结果覆盖掉。
        let mut open = self.settings_open;
        let mut save = false;
        let mut cancel = false;
        egui::Window::new(s.settings_title)
            .collapsible(false)
            .resizable(false)
            .anchor(egui::Align2::CENTER_CENTER, Vec2::ZERO)
            .open(&mut open)
            .show(ctx, |ui| {
                ui.set_width(340.0);
                ui.add_space(4.0);

                // 语言
                ui.label(RichText::new(s.settings_lang).color(theme::DIM).size(12.0));
                egui::ComboBox::from_id_salt("lang_select")
                    .width(ui.available_width())
                    .selected_text(lang_label(&self.pending_lang, &s))
                    .show_ui(ui, |ui| {
                        ui.selectable_value(
                            &mut self.pending_lang,
                            LANG_SYSTEM.to_string(),
                            s.lang_system,
                        );
                        ui.selectable_value(&mut self.pending_lang, "zho".to_string(), s.lang_zh);
                        ui.selectable_value(&mut self.pending_lang, "eng".to_string(), s.lang_en);
                    });

                ui.add_space(12.0);

                // 铃声文件
                ui.label(RichText::new(s.settings_audio).color(theme::DIM).size(12.0));
                let audio_label = if self.pending_audio.is_empty()
                    || self.pending_audio == "#default_01"
                {
                    s.settings_default_audio.to_string()
                } else {
                    self.pending_audio.clone()
                };
                ui.horizontal(|ui| {
                    if ui.button(s.settings_browse).clicked() {
                        if let Some(path) = rfd::FileDialog::new()
                            .add_filter("Audio", &["mp3", "wav", "ogg"])
                            .pick_file()
                        {
                            self.pending_audio = path.to_string_lossy().to_string();
                        }
                    }
                    if ui.button(s.settings_reset).clicked() {
                        self.pending_audio = "#default_01".to_string();
                    }
                });
                ui.label(RichText::new(audio_label).color(theme::DIM).size(11.0));

                ui.add_space(16.0);
                ui.horizontal(|ui| {
                    save = ui.button(s.save).clicked();
                    cancel = ui.button(s.cancel).clicked();
                });
            });
        if save {
            self.save_settings();
            open = false;
        }
        if cancel {
            open = false;
        }
        self.settings_open = open;
    }
}

/// 标题栏图标按钮（自绘悬停底色 + 图标）。
fn icon_button(ui: &mut egui::Ui, tex: &egui::TextureHandle, tip: &str) -> egui::Response {
    let (rect, resp) = ui.allocate_exact_size(Vec2::splat(26.0), Sense::click());
    if resp.hovered() {
        ui.painter().rect_filled(rect, 5.0, theme::HOVER);
    }
    let icon = egui::Rect::from_center_size(rect.center(), Vec2::splat(17.0));
    egui::Image::new(egui::load::SizedTexture::from_handle(tex)).paint_at(ui, icon);
    resp.on_hover_text(tip)
}

/// 主操作按钮（开始/取消），占满一行。
fn big_button(
    ui: &mut egui::Ui,
    label: &str,
    fill: Color32,
    hover: Color32,
    width: f32,
    height: f32,
) -> egui::Response {
    let (rect, resp) = ui.allocate_exact_size(Vec2::new(width, height), Sense::click());
    let bg = if resp.hovered() { hover } else { fill };
    ui.painter().rect_filled(rect, 8.0, bg);
    ui.painter().text(
        rect.center(),
        egui::Align2::CENTER_CENTER,
        label,
        FontId::proportional(17.0),
        Color32::WHITE,
    );
    resp
}

/// 在滑块下方绘制刻度线与刻度标签（位置与滑块值线性对应）。
fn draw_ticks(ui: &mut egui::Ui, slider: &egui::Response, current: u32, labels: &[&str; 7]) {
    /// 刻度对应的滑块值，与 locale `timeSlider` 的键一致。
    const POS: [u32; 7] = [0, 30, 60, 84, 102, 126, 150];

    let rect = slider.rect;
    let (row, _) = ui.allocate_exact_size(Vec2::new(rect.width(), 22.0), Sense::hover());
    let painter = ui.painter();

    for (i, pos) in POS.iter().enumerate() {
        let x = row.left() + (*pos as f32 / SLIDER_MAX as f32) * row.width();
        painter.line_segment(
            [egui::pos2(x, row.top()), egui::pos2(x, row.top() + 4.0)],
            Stroke::new(1.0_f32, theme::BORDER),
        );

        // 首尾标签分别左/右对齐，避免超出窗口边界被裁掉
        let (anchor, text_x) = if i == 0 {
            (egui::Align2::LEFT_TOP, row.left())
        } else if i == POS.len() - 1 {
            (egui::Align2::RIGHT_TOP, row.right())
        } else {
            (egui::Align2::CENTER_TOP, x)
        };

        // 靠近当前值的刻度高亮，便于一眼看出所处区间
        let near = current.abs_diff(*pos) <= 3;
        let color = if near { theme::TEXT } else { theme::DIM };
        painter.text(
            egui::pos2(text_x, row.top() + 6.0),
            anchor,
            labels[i],
            FontId::proportional(10.0),
            color,
        );
    }
}

pub fn strings_for_lang(lang: &str) -> Strings {
    match lang {
        "eng" => Strings::en(),
        "zho" => Strings::zh(),
        _ => {
            // 跟随系统：按 LANG / LC_ALL 判断是否为中文环境
            let sys = std::env::var("LANG")
                .or_else(|_| std::env::var("LC_ALL"))
                .unwrap_or_default();
            if sys.to_ascii_lowercase().contains("zh") {
                Strings::zh()
            } else {
                Strings::en()
            }
        }
    }
}

fn lang_label(lang: &str, s: &Strings) -> String {
    match lang {
        "zho" => s.lang_zh.to_string(),
        "eng" => s.lang_en.to_string(),
        _ => s.lang_system.to_string(),
    }
}
