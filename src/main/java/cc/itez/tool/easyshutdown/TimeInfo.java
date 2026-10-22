package cc.itez.tool.easyshutdown;

/**
 * 滑块刻度到秒数的不可变映射。
 *
 * | 刻度区间    | 对应时长   | 步长     |
 * | 0   - 60  | 0 - 1 小时 | 1 分钟  |
 * | 61  - 84  | 1 - 3 小时 | 5 分钟  |
 * | 85  - 102 | 3 - 6 小时 | 10 分钟 |
 * | 103 - 126 | 6 - 12 小时| 15 分钟 |
 * | 127 - 150 | 12-24 小时 | 30 分钟 |
 *
 * 不可变设计：倒计时的可变状态由 CountdownService 持有，本类只做值映射与格式化。
 */
public final class TimeInfo {

    private final int seconds;

    private TimeInfo(int seconds) {
        this.seconds = seconds;
    }

    public static TimeInfo fromSliderValue(double value) {
        int val = (int) value;
        int seconds;
        if (val <= 60) {
            seconds = val * 60;
        } else if (val <= 84) {
            seconds = 3600 + (val - 60) * 5 * 60;
        } else if (val <= 102) {
            seconds = 3 * 3600 + (val - 84) * 10 * 60;
        } else if (val <= 126) {
            seconds = 6 * 3600 + (val - 102) * 15 * 60;
        } else {
            seconds = 12 * 3600 + (val - 126) * 30 * 60;
        }
        return new TimeInfo(seconds);
    }

    public int seconds() {
        return seconds;
    }

    public String formatted() {
        return format(seconds);
    }

    /**
     * 将秒数格式化为 HH:mm:ss
     */
    public static String format(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
