package cc.itez.tool.easyshutdown;

public class TimeInfo {
    String number;
    Integer second;
    Double pointer;

    public String number() {
        return number;
    }

    public void number(String number) {
        this.number = number;
    }

    public Integer second() {
        return second;
    }

    public TimeInfo secondDecrement() {
        this.number = this.formatterSecond(--this.second);
        return this;
    }

    /**
     * | 时间(小时) | 跨度(分钟) | 步长(分钟) | 刻度数 | 刻度位置 |
     * |--------|--------|--------|-----|------|
     * | 1      | 60     | 1      | 60  | 60   |
     * | 3      | 120    | 5      | 24  | 84   |
     * | 6      | 180    | 10     | 18  | 102  |
     * | 12     | 360    | 15     | 24  | 126  |
     * | 24     | 720    | 30     | 24  | 150  |
     *
     * @param value 刻度位置
     */
    public TimeInfo(double value) {
        int val = (int) value;
        this.pointer = value;
        if (val <= 60) {
            this.second = val * 60;
            this.number = this.formatterSecond(this.second);
        } else if (val <= 84) {
            int nv = val - 60;
            this.second = 3600 + nv * 5 * 60;
            this.number = this.formatterSecond(this.second);
        } else if (val <= 102) {
            int nv = val - 84;
            this.second = (3 * 3600) + nv * 10 * 60;
            this.number = this.formatterSecond(this.second);
        } else if (val <= 126) {
            int nv = val - 102;
            this.second = (6 * 3600) + nv * 15 * 60;
            this.number = this.formatterSecond(this.second);
        } else if (val <= 150) {
            int nv = val - 126;
            this.second = (12 * 3600) + nv * 30 * 60;
            this.number = this.formatterSecond(this.second);
        }
    }

    private String formatterSecond(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
