package cc.itez.tool.easyshutdown;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 轻量日志工具。
 * 线程安全：DateTimeFormatter 不可变，minLevel 用 volatile 保证可见性。
 */
public final class Logger {

    public enum Level {DEBUG, INFO, WARN, ERROR}

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static volatile Level minLevel = Level.DEBUG;

    private Logger() {
    }

    public static void setMinLevel(Level level) {
        minLevel = level;
    }

    public static void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    public static void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public static void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    public static void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    public static void error(Throwable e, String message, Object... args) {
        log(Level.ERROR, message, args);
        log(Level.ERROR, stackTraceToString(e));
    }

    private static void log(Level level, String message, Object... args) {
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }
        System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] "
                + String.format("[%-5s] ", level) + format(message, args));
    }

    /**
     * 使用 {} 作为占位符，依次将 args 替换进 message；多余的占位符替换为 ?
     */
    private static String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        StringBuilder formatted = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < message.length(); i++) {
            char currentChar = message.charAt(i);
            if (currentChar == '{' && i + 1 < message.length() && message.charAt(i + 1) == '}') {
                formatted.append(argIndex < args.length ? args[argIndex++] : "?");
                i++; // 跳过右括号
                continue;
            }
            formatted.append(currentChar);
        }
        return formatted.toString();
    }

    private static String stackTraceToString(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwable).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            builder.append("\tat ").append(element).append("\n");
        }
        return builder.toString();
    }
}
