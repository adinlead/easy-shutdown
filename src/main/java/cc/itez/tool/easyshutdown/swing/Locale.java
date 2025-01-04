package cc.itez.tool.easyshutdown.swing;

import cc.itez.tool.easyshutdown.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

public class Locale {

    /**
     * 根据语言获取本地化配置
     * 加载顺序为：外部配置资源 -> 内部配置资源 -> 默认配置资源
     *
     * @param langCode 语言代码
     * @return 本地化配置
     */
    public static Locale get(String langCode) {
        Gson gson = new GsonBuilder().create();
        if (langCode != null && !(langCode = langCode.toLowerCase()).isEmpty()) {
            String filePath = "/locale/" + langCode + ".json";
            File outerResource = new File(filePath);
            if (outerResource.exists()) {
                try (FileReader fileReader = new FileReader(outerResource)) {
                    return gson.fromJson(fileReader, Locale.class);
                } catch (IOException e) {
                    Logger.error(e, "获取外部配置文件【{}】出错", filePath);
                }
            }
            filePath = "/locale/" + langCode + ".json";
            URL innerResource = Locale.class.getResource(filePath);
            if (innerResource != null) {
                try (InputStream stream = innerResource.openStream()) {
                    try (InputStreamReader reader = new InputStreamReader(stream)) {
                        return gson.fromJson(reader, Locale.class);
                    }
                } catch (IOException e) {
                    Logger.error(e, "获取内部配置文件【{}】出错", filePath);
                }
            }
        }
        try (InputStream stream = Resource.LANG_DEFAULT.stream()) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                return gson.fromJson(reader, Locale.class);
            }
        } catch (IOException e) {
            Logger.error(e, "获取默认配置文件【{}】出错", Resource.LANG_DEFAULT.path());
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> locales() {
        Gson gson = new GsonBuilder().create();
        Map<String, String> locales = new TreeMap<>();

        for (Resource resource : Resource.values()) {
            if (resource.name().startsWith("LANG_")) {
                try (InputStream stream = resource.stream()) {
                    try (InputStreamReader reader = new InputStreamReader(stream)) {
                        Locale locale = gson.fromJson(reader, Locale.class);
                        locales.put(locale.getLangName(), locale.getLangCode());
                    }
                } catch (IOException e) {
                    Logger.error(e, "获取内部配置文件【{}】出错", resource.path());
                }
            }
        }

        // 指定要遍历的目录路径
        File directoryPath = new File("locale/");
        File[] listFiles = directoryPath.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    try (FileReader reader = new FileReader(file)) {
                        Locale locale = gson.fromJson(reader, Locale.class);
                        locales.put(locale.getLangName(), locale.getLangCode());
                    } catch (IOException e) {
                        Logger.error(e, "获取内部配置文件【{}】出错", file.getPath());
                    }
                }
            }
        }
        return locales;
    }

    private String langName;
    private String langCode;
    private String title;
    private String description;
    private TitleBar titleBar;
    private TimeSlider timeSlider;
    private ActionButtons actionButtons;
    private Tray tray;

    public String getLangName() {
        return langName;
    }

    public void setLangName(String langName) {
        this.langName = langName;
    }

    public String getLangCode() {
        return langCode;
    }

    public void setLangCode(String langCode) {
        this.langCode = langCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TitleBar getTitleBar() {
        return titleBar;
    }

    public void setTitleBar(TitleBar titleBar) {
        this.titleBar = titleBar;
    }

    public TimeSlider getTimeSlider() {
        return timeSlider;
    }

    public void setTimeSlider(TimeSlider timeSlider) {
        this.timeSlider = timeSlider;
    }

    public ActionButtons getActionButtons() {
        return actionButtons;
    }

    public void setActionButtons(ActionButtons actionButtons) {
        this.actionButtons = actionButtons;
    }

    public Tray getTrayMenus() {
        return tray;
    }

    public void setTrayMenus(Tray tray) {
        this.tray = tray;
    }

    public static class TitleBar {
        private String title;
        private String tray;
        private String setting;
        private String close;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTray() {
            return tray;
        }

        public void setTray(String tray) {
            this.tray = tray;
        }

        public String getSetting() {
            return setting;
        }

        public void setSetting(String setting) {
            this.setting = setting;
        }

        public String getClose() {
            return close;
        }

        public void setClose(String close) {
            this.close = close;
        }
    }

    public static class TimeSlider {
        private String scale0;
        private String scale30;
        private String scale60;
        private String scale84;
        private String scale102;
        private String scale126;
        private String scale150;

        public String getScale0() {
            return scale0;
        }

        public void setScale0(String scale0) {
            this.scale0 = scale0;
        }

        public String getScale30() {
            return scale30;
        }

        public void setScale30(String scale30) {
            this.scale30 = scale30;
        }

        public String getScale60() {
            return scale60;
        }

        public void setScale60(String scale60) {
            this.scale60 = scale60;
        }

        public String getScale84() {
            return scale84;
        }

        public void setScale84(String scale84) {
            this.scale84 = scale84;
        }

        public String getScale102() {
            return scale102;
        }

        public void setScale102(String scale102) {
            this.scale102 = scale102;
        }

        public String getScale126() {
            return scale126;
        }

        public void setScale126(String scale126) {
            this.scale126 = scale126;
        }

        public String getScale150() {
            return scale150;
        }

        public void setScale150(String scale150) {
            this.scale150 = scale150;
        }
    }

    public static class ActionButtons {
        private String shutdown;
        private String restart;
        private String ringing;

        public String getShutdown() {
            return shutdown;
        }

        public void setShutdown(String shutdown) {
            this.shutdown = shutdown;
        }

        public String getRestart() {
            return restart;
        }

        public void setRestart(String restart) {
            this.restart = restart;
        }

        public String getRinging() {
            return ringing;
        }

        public void setRinging(String ringing) {
            this.ringing = ringing;
        }
    }

    public static class Tray {
        private String tooltip;
        private String exit;

        public String getTooltip() {
            return tooltip;
        }

        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        public String getExit() {
            return exit;
        }

        public void setExit(String exit) {
            this.exit = exit;
        }
    }
}
