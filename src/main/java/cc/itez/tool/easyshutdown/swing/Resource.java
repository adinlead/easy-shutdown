package cc.itez.tool.easyshutdown.swing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 资源枚举类，用于定义和管理应用程序中的静态资源
 * 包括图像、字体和声音文件等资源的路径
 */
public enum Resource {
    /** 32像素的LOGO图像 **/
    IMG_LOGO_32("/img/logo/logo-32.png"),
    IMG_LOGO_48("/img/logo/logo-48.png"),
    IMG_LOGO_64("/img/logo/logo-64.png"),
    IMG_LOGO_512("/img/logo/logo-512.png"),
    /** 标题栏-托盘按钮图像 **/
    IMG_BAR_BTN_TRAY("/img/act_btn/tray.png"),
    /** 标题栏-设置按钮图像 **/
    IMG_BAR_BTN_SETTING("/img/act_btn/setting.png"),
    /** 标题栏-关闭按钮图像 **/
    IMG_BAR_BTN_CLOSE("/img/act_btn/close.png"),
    /** 倒计时数字字体 **/
    FONT_NUMBER_DEFAULT("/fonts/number-default.ttf"),
    /** 默认文本字体 **/
    FONT_TEXT_DEFAULT("/fonts/WQY-WeiMiHei.ttf"),
    /** 默认声音文件 **/
    SOUND_DEFAULT("/sounds/morning-joy.mp3"),
    /** 默认语言 **/
    LANG_DEFAULT("/locale/zho.json");

    // 资源路径私有变量
    private final String path;

    /**
     * 获取资源路径
     *
     * @return 资源路径字符串
     */
    public String path() {
        return path;
    }

    /**
     * 获取资源的URL
     *
     * @return 资源的URL对象
     */
    public URL url() {
        return getClass().getResource(this.path);
    }

    /**
     * 枚举构造函数，初始化资源路径
     *
     * @param path 资源路径字符串
     */
    Resource(String path) {
        this.path = path;
    }

    /**
     * 获取资源的输入流
     *
     * @return 资源的InputStream对象
     * @throws IOException 如果无法创建输入流，则抛出IOException
     */
    public InputStream stream() throws IOException {
        return url().openStream();
    }
}

