package cc.itez.tool.easyshutdown.core;

import java.io.IOException;

/**
 * macOS 关机/重启：通过 AppleScript 请求 System Events，
 * 在图形桌面会话下无需 root（会触发系统标准的关机确认流程）。
 */
final class MacOperator implements SystemOperator {

    @Override
    public void shutdown() {
        run("shut down");
    }

    @Override
    public void reboot() {
        run("restart");
    }

    private static void run(String action) {
        try {
            new ProcessBuilder("osascript", "-e",
                    "tell application \"System Events\" to " + action).start();
        } catch (IOException e) {
            throw new OperationException("无法执行 osascript", e);
        }
    }
}
