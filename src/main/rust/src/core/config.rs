//! 应用配置（对应 Java 版 `AppConfig` / `java.util.prefs`）。
//!
//! 零第三方依赖：以 `key = value` 行格式存储于各平台标准配置目录：
//! - Windows:      `%APPDATA%\easy-shutdown\config.ini`
//! - Linux:        `$XDG_CONFIG_HOME/easy-shutdown/config.ini`（默认 `~/.config`）
//! - macOS:        `~/Library/Application Support/easy-shutdown/config.ini`

use std::fs;
use std::path::PathBuf;

use crate::core::error::OperationError;

/// 语言设置默认值：跟随系统
pub const LANG_SYSTEM: &str = "#System";
/// 音频文件默认值：内置音频
const DEFAULT_AUDIO_FILE: &str = "#default_01";

const LANG_KEY: &str = "lang";
const AUDIO_FILE_KEY: &str = "audio_file";

pub struct AppConfig {
    path: PathBuf,
    lang: String,
    audio_file: String,
}

impl AppConfig {
    /// 从平台标准位置加载配置；文件不存在或字段缺失时使用默认值。
    pub fn load() -> Self {
        Self::load_from(config_file_path())
    }

    fn load_from(path: PathBuf) -> Self {
        match fs::read_to_string(&path) {
            Ok(content) => {
                let mut cfg = Self::with_defaults(path);
                for line in content.lines() {
                    let line = line.trim();
                    if line.is_empty() || line.starts_with('#') {
                        continue;
                    }
                    if let Some((key, value)) = line.split_once('=') {
                        let value = value.trim().to_string();
                        match key.trim() {
                            LANG_KEY => cfg.lang = value,
                            AUDIO_FILE_KEY => cfg.audio_file = value,
                            _ => {}
                        }
                    }
                }
                cfg
            }
            // 文件不存在 / 无权限读取：降级为默认配置，不致命
            Err(_) => Self::with_defaults(path),
        }
    }

    pub fn lang(&self) -> &str {
        &self.lang
    }

    pub fn set_lang(&mut self, lang: impl Into<String>) {
        self.lang = lang.into();
    }

    pub fn audio_file(&self) -> &str {
        &self.audio_file
    }

    pub fn set_audio_file(&mut self, audio_file: impl Into<String>) {
        self.audio_file = audio_file.into();
    }

    /// 持久化到磁盘。写入失败向上返回，由 UI 边界提示。
    pub fn save(&self) -> Result<(), OperationError> {
        self.save_to(&self.path)
    }

    fn save_to(&self, path: &PathBuf) -> Result<(), OperationError> {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)
                .map_err(|e| OperationError::with_source(format!("无法创建配置目录 {parent:?}"), e))?;
        }
        let content = format!("{LANG_KEY}={}\n{AUDIO_FILE_KEY}={}\n", self.lang, self.audio_file);
        // 先写临时文件再 rename，避免写入中途崩溃留下半截配置
        let tmp = path.with_extension("ini.tmp");
        fs::write(&tmp, content)
            .map_err(|e| OperationError::with_source(format!("无法写入配置文件 {path:?}"), e))?;
        let _ = fs::remove_file(path); // rename 在 Windows 上不覆盖已存在文件
        fs::rename(&tmp, path)
            .map_err(|e| OperationError::with_source(format!("无法替换配置文件 {path:?}"), e))
    }

    fn with_defaults(path: PathBuf) -> Self {
        Self { path, lang: LANG_SYSTEM.to_string(), audio_file: DEFAULT_AUDIO_FILE.to_string() }
    }
}

fn config_file_path() -> PathBuf {
    config_dir().join("easy-shutdown").join("config.ini")
}

fn config_dir() -> PathBuf {
    if cfg!(windows) {
        // %APPDATA%（Roaming）
        std::env::var_os("APPDATA")
            .map(PathBuf::from)
            .unwrap_or_else(home_dir_fallback)
    } else if cfg!(target_os = "macos") {
        home_dir_fallback().join("Library").join("Application Support")
    } else {
        // XDG Base Directory
        std::env::var_os("XDG_CONFIG_HOME")
            .map(PathBuf::from)
            .filter(|p| p.is_absolute())
            .unwrap_or_else(|| home_dir_fallback().join(".config"))
    }
}

fn home_dir_fallback() -> PathBuf {
    let home = if cfg!(windows) {
        std::env::var_os("USERPROFILE")
    } else {
        std::env::var_os("HOME")
    };
    PathBuf::from(home.unwrap_or_else(|| PathBuf::from(".").into_os_string()))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_path(tag: &str) -> PathBuf {
        std::env::temp_dir()
            .join(format!("easy-shutdown-test-{}-{tag}.ini", std::process::id()))
    }

    #[test]
    fn missing_file_falls_back_to_defaults() {
        let cfg = AppConfig::load_from(temp_path("missing"));
        assert_eq!(cfg.lang(), LANG_SYSTEM);
        assert_eq!(cfg.audio_file(), DEFAULT_AUDIO_FILE);
    }

    #[test]
    fn save_and_load_roundtrip() {
        let path = temp_path("roundtrip");
        let mut cfg = AppConfig::load_from(path.clone());
        cfg.set_lang("zho");
        cfg.set_audio_file("/music/ring.mp3");
        cfg.save_to(&path).expect("save should succeed");

        let loaded = AppConfig::load_from(path.clone());
        assert_eq!(loaded.lang(), "zho");
        assert_eq!(loaded.audio_file(), "/music/ring.mp3");

        let _ = fs::remove_file(&path);
    }

    #[test]
    fn comments_and_blank_lines_are_ignored() {
        let path = temp_path("comments");
        fs::write(&path, "# comment\n\nlang = eng\nunknown_key=x\n")
            .expect("write fixture");
        let cfg = AppConfig::load_from(path.clone());
        assert_eq!(cfg.lang(), "eng");
        assert_eq!(cfg.audio_file(), DEFAULT_AUDIO_FILE);
        let _ = fs::remove_file(&path);
    }
}
