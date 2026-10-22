package cc.itez.tool.easyshutdown.core;

/**
 * 核心层统一异常：底层错误（系统命令、音频、配置）包装后向 UI 边界抛出，
 * 由 UI 层统一捕获并给出用户可见的反馈。
 */
public class OperationException extends RuntimeException {
    public OperationException(String message) {
        super(message);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
