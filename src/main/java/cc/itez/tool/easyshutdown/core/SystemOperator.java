package cc.itez.tool.easyshutdown.core;

/**
 * 系统级操作（关机/重启）的抽象，按操作系统分派实现。
 * 所有实现都尽量走"免 root/免 UAC"的官方路径。
 */
public interface SystemOperator {

    void shutdown();

    void reboot();

    /**
     * 按当前操作系统返回对应实现
     */
    static SystemOperator current() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new WindowsOperator();
        }
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return new LinuxOperator();
        }
        if (os.contains("mac")) {
            return new MacOperator();
        }
        throw new OperationException("Unsupported operating system: " + os);
    }
}
