package cc.itez.tool.easyshutdown;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    public static final StringBuffer buffer = new StringBuffer();
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final String LEVEL_DEBUG = "DEBUG";
    public static final String LEVEL_INFO = "INFO ";
    public static final String LEVEL_WARN = "WARN ";
    public static final String LEVEL_ERROR = "ERROR";

    private static String formatter(String message, Object... args) {
        // 如果args为空，则直接返回message
        // 使用{}作为占位符，依次将args中的参数替换入message中
        // 如果args的长度小于message中占位符的数量，则将多余的占位符替换为问号
        // 如果args的长度大于message中占位符的数量，则将多余的参数转化为String后依次拼接到message末尾，注意多余的参数转化为String时，需要在String前添加多余参数在args中序号
        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder formattedMessage = new StringBuilder();
        int argIndex = 0;

        for (int i = 0; i < message.length(); i++) {
            char currentChar = message.charAt(i);

            if (currentChar == '{') {
                if (i + 1 < message.length() && message.charAt(i + 1) == '}') {
                    if (argIndex < args.length) {
                        formattedMessage.append(args[argIndex]);
                        argIndex++;
                    } else {
                        formattedMessage.append('?');
                    }
                    i++; // Skip the closing brace
                    continue;
                }
            }

            formattedMessage.append(currentChar);
        }

        // Append remaining args with their indices
        if (argIndex < args.length) {
            formattedMessage.append(" | MORE ==> {");
            for (int i = argIndex; i < args.length; i++) {
                formattedMessage.append("[").append(i).append("]").append(args[i]).append(", ");
            }
            formattedMessage.replace(formattedMessage.length() - 2, formattedMessage.length(), "}");
        }

        return formattedMessage.toString();
    }

    public static void setDateFormatter(String dateFormatter) {
        Logger.dateFormatter.applyPattern(dateFormatter);
    }

    public static void out(String content) {
        System.out.println(content);
        buffer.append(content).append("\n");
    }

    public static void clearBuffer() {
        int n = 1024 * 1024;
        if (buffer == null || n <= 0) {
            buffer.setLength(0); // 清空缓冲区
            return;
        }

        int length = buffer.length();
        if (n >= length) {
            return; // 如果 n 大于或等于当前长度，不需要删除任何内容
        }

        int startIndex = length - n;
        buffer.delete(0, startIndex);
    }

    private static void log(String level, String message, Object... args) {
        if (args != null && args.length > 0) {
            out("[" + dateFormatter.format(new Date()) + "] [" + level + "] " + formatter(message, args));
        } else {
            out("[" + dateFormatter.format(new Date()) + "] [" + level + "] " + message);
        }
    }

    public static void debug(String message, Object... args) {
        log(LEVEL_DEBUG, message, args);
    }

    public static void info(String message, Object... args) {
        log(LEVEL_INFO, message, args);
    }

    public static void warn(String message, Object... args) {
        log(LEVEL_WARN, message, args);
    }

    public static void error(String message, Object... args) {
        log(LEVEL_ERROR, message, args);
    }

    private static String stackTraceToString(Throwable throwable) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Exception occurred:\n");
        buffer.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            buffer.append("\tat ").append(element).append("\n");
        }
        return buffer.toString();
    }

    public static void error(Exception e, String message, Object... args) {
        log(LEVEL_ERROR, message, args);
        log(LEVEL_ERROR, stackTraceToString(e));
    }
}
