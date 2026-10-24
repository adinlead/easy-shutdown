//! EasyShutdown 核心层（对应 Java 版 `cc.itez.tool.easyshutdown.core`）。
//!
//! 本 crate 只包含与 UI 无关的功能代码；UI（egui）将在后续步骤接入。
//!
//! 注意：模块名沿用 Java 包名 `core`。Rust 标准库的 core crate 不会被遮蔽，
//! 但若需要引用标准库 core，请使用全路径 `::core`（本 crate 代码无需如此）。

pub mod core;
