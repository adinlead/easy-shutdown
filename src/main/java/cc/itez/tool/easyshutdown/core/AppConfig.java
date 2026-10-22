package cc.itez.tool.easyshutdown.core;

import cc.itez.tool.easyshutdown.Logger;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * 应用配置，基于 JDK Preferences 持久化：
 * Windows 存注册表（HKCU\Software\JavaSoft\Prefs），Linux 存 ~/.java/.userPrefs。
 * 零依赖、天然跨平台，无需手动管理配置文件路径。
 */
public final class AppConfig {

    /** 语言设置的默认值：跟随系统 */
    public static final String LANG_SYSTEM = "#System";

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppConfig.class);

    /** 语言代码（如 zho/eng），默认跟随系统 */
    public String lang() {
        return PREFS.get("lang", LANG_SYSTEM);
    }

    public void lang(String langCode) {
        PREFS.put("lang", langCode);
        flush();
    }

    /** 音频文件标识，默认使用内置音频 */
    public String audioFile() {
        return PREFS.get("audioFile", "#default_01");
    }

    public void audioFile(String audioFile) {
        PREFS.put("audioFile", audioFile);
        flush();
    }

    private static void flush() {
        try {
            PREFS.flush();
        } catch (BackingStoreException e) {
            Logger.error(e, "配置写入持久层失败");
        }
    }
}
