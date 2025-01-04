package cc.itez.tool.easyshutdown.swing;

import javax.swing.*;
import java.io.File;

public class Setting {
    private static final File configFile = new File("config.json");
    /** 音频文件位置 **/
    private String audioFile = "#default_01";
    /** 语言 **/
    private String lang = "#System";


    /**
     * 主窗口
     */
    private final JFrame mainFrame;

    public Setting(JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * 从磁盘读取配置
     */
    public void load() {

    }

    /**
     * 将配置写入磁盘
     */
    public void dump() {

    }

    /**
     * 弹出配置窗口
     */
    public void popup() {

    }
}
