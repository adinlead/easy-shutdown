//! 非线性时间刻度映射 + 时长格式化（对应 Java 版 `TimeInfo`）。
//!
//! 滑块刻度 0–150 非线性映射到分钟：
//! - 0–60：每刻度 1 分钟
//! - 60–84：每刻度 5 分钟
//! - 84–96：每刻度 10 分钟
//! - 96–126：每刻度 15 分钟
//! - 126–150：每刻度 30 分钟
//!
//! 短区间精度高、长区间跨度大，适配"定时关机"的使用习惯。

/// 滑块最大值（含）。
pub const SLIDER_MAX: u32 = 150;

/// 滑块刻度 → 分钟数。
pub fn slider_to_minutes(pointer: u32) -> u32 {
    let pointer = pointer.clamp(0, SLIDER_MAX);
    match pointer {
        0..=60 => pointer,
        61..=84 => 60 + (pointer - 60) * 5,
        85..=96 => 180 + (pointer - 84) * 10,
        97..=126 => 300 + (pointer - 96) * 15,
        _ => 750 + (pointer - 126) * 30,
    }
}

/// 分钟数 → 滑块刻度（反向映射，用于从配置恢复位置）。
pub fn minutes_to_slider(minutes: u32) -> u32 {
    match minutes {
        0..=60 => minutes,
        61..=180 => 60 + (minutes - 60) / 5,
        181..=300 => 84 + (minutes - 180) / 10,
        301..=750 => 96 + (minutes - 300) / 15,
        _ => 126 + (minutes - 750) / 30,
    }
}

/// 总秒数格式化为 `HH:MM:SS`。
pub fn format_duration(total_secs: u32) -> String {
    let h = total_secs / 3600;
    let m = (total_secs % 3600) / 60;
    let s = total_secs % 60;
    format!("{:02}:{:02}:{:02}", h, m, s)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn mapping_is_monotonic() {
        let mut prev = 0;
        for p in 0..=SLIDER_MAX {
            let mins = slider_to_minutes(p);
            assert!(mins >= prev, "non-monotonic at {p}");
            prev = mins;
        }
    }

    #[test]
    fn endpoints() {
        assert_eq!(slider_to_minutes(0), 0);
        assert_eq!(slider_to_minutes(60), 60);
        assert_eq!(slider_to_minutes(84), 180);
        assert_eq!(slider_to_minutes(96), 300);
        assert_eq!(slider_to_minutes(126), 750);
        assert_eq!(slider_to_minutes(150), 1470);
    }

    #[test]
    fn format_pads() {
        assert_eq!(format_duration(0), "00:00:00");
        assert_eq!(format_duration(61), "00:01:01");
        assert_eq!(format_duration(3661), "01:01:01");
    }
}
