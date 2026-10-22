package cc.itez.tool.easyshutdown.core;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;

/**
 * Windows 关机/重启：直接调用 Win32 ExitWindowsEx，而不是派生 shutdown.exe 子进程。
 * 交互式登录用户默认就拥有 SE_SHUTDOWN_NAME 权限（只是处于禁用状态），
 * 通过 AdjustTokenPrivileges 启用后即可直接调用，全程无需 UAC 提权、无子进程开销。
 */
final class WindowsOperator implements SystemOperator {

    @Override
    public void shutdown() {
        exitWindows(WinUser.EWX_SHUTDOWN);
    }

    @Override
    public void reboot() {
        exitWindows(WinUser.EWX_REBOOT);
    }

    private static void exitWindows(int flag) {
        enableShutdownPrivilege();
        WinDef.BOOL result = User32.INSTANCE.ExitWindowsEx(
                new WinDef.UINT(flag | WinUser.EWX_FORCEIFHUNG), new WinNT.DWORD(0));
        if (result == null || !result.booleanValue()) {
            throw new OperationException("ExitWindowsEx failed, error=" + Kernel32.INSTANCE.GetLastError());
        }
    }

    private static void enableShutdownPrivilege() {
        WinNT.HANDLEByReference tokenRef = new WinNT.HANDLEByReference();
        if (!Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),
                WinNT.TOKEN_ADJUST_PRIVILEGES | WinNT.TOKEN_QUERY, tokenRef)) {
            throw new OperationException("OpenProcessToken failed, error=" + Kernel32.INSTANCE.GetLastError());
        }
        WinNT.HANDLE token = tokenRef.getValue();
        try {
            WinNT.LUID luid = new WinNT.LUID();
            if (!Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_SHUTDOWN_NAME, luid)) {
                throw new OperationException("LookupPrivilegeValue failed, error=" + Kernel32.INSTANCE.GetLastError());
            }
            WinNT.TOKEN_PRIVILEGES privileges = new WinNT.TOKEN_PRIVILEGES(1);
            privileges.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new WinNT.DWORD(WinNT.SE_PRIVILEGE_ENABLED));
            Advapi32.INSTANCE.AdjustTokenPrivileges(token, false, privileges, 0, null, null);
            if (Kernel32.INSTANCE.GetLastError() != WinNT.ERROR_SUCCESS) {
                throw new OperationException("AdjustTokenPrivileges failed, error=" + Kernel32.INSTANCE.GetLastError());
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(token);
        }
    }
}
