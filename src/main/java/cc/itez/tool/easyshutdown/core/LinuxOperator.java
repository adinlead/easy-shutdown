package cc.itez.tool.easyshutdown.core;

import java.io.IOException;

/**
 * Linux 关机/重启：通过 systemctl 走 systemd-logind + polkit。
 * 对活跃的本地桌面会话，polkit 默认策略（allow_active=yes）免密放行，
 * 与桌面环境的关机按钮走的是同一条路径，因此无需 root。
 */
final class LinuxOperator implements SystemOperator {

    @Override
    public void shutdown() {
        run("systemctl", "poweroff");
    }

    @Override
    public void reboot() {
        run("systemctl", "reboot");
    }

    private static void run(String... cmd) {
        try {
            int exitCode = new ProcessBuilder(cmd).start().waitFor();
            if (exitCode != 0) {
                throw new OperationException(String.join(" ", cmd) + " exited with code " + exitCode
                        + " (可能需要 polkit 交互式授权，例如 SSH 会话)");
            }
        } catch (IOException e) {
            throw new OperationException("无法执行 " + cmd[0] + "，当前系统可能未使用 systemd", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
