//! 系统级操作（关机/重启），对应 Java 版 `SystemOperator` 接口。
//!
//! 区别在于：Java 版靠运行时判断 `os.name` 分派实现，
//! Rust 版在编译期用 `#[cfg]` 分派——目标系统在构建时即已确定，
//! 无关平台的代码根本不会参与编译。

use crate::core::error::OperationError;

/// 关机。各平台实现均走"免提权"路径，与桌面环境的关机按钮等效。
pub fn shutdown() -> Result<(), OperationError> {
    #[cfg(target_os = "windows")]
    return windows_impl::shutdown();
    #[cfg(target_os = "linux")]
    return linux_impl::shutdown();
    #[cfg(target_os = "macos")]
    return macos_impl::shutdown();
    #[cfg(not(any(target_os = "windows", target_os = "linux", target_os = "macos")))]
    Err(OperationError::new(format!(
        "unsupported operating system: {}",
        std::env::consts::OS
    )))
}

/// 重启。
pub fn reboot() -> Result<(), OperationError> {
    #[cfg(target_os = "windows")]
    return windows_impl::reboot();
    #[cfg(target_os = "linux")]
    return linux_impl::reboot();
    #[cfg(target_os = "macos")]
    return macos_impl::reboot();
    #[cfg(not(any(target_os = "windows", target_os = "linux", target_os = "macos")))]
    Err(OperationError::new(format!(
        "unsupported operating system: {}",
        std::env::consts::OS
    )))
}

// ---------------------------------------------------------------------------
// Windows：直接调 Win32 ExitWindowsEx，不经 shutdown.exe 子进程。
// 对应 Java 版 WindowsOperator（JNA 版本的移植）。
// ---------------------------------------------------------------------------
#[cfg(target_os = "windows")]
mod windows_impl {
    use super::OperationError;
    use windows::Win32::Foundation::{CloseHandle, GetLastError, ERROR_NOT_ALL_ASSIGNED, HANDLE, LUID};
    use windows::Win32::Security::{
        AdjustTokenPrivileges, LookupPrivilegeValueW, LUID_AND_ATTRIBUTES, SE_PRIVILEGE_ENABLED,
        SE_SHUTDOWN_NAME, TOKEN_ADJUST_PRIVILEGES, TOKEN_PRIVILEGES, TOKEN_QUERY,
    };
    use windows::Win32::System::Threading::{GetCurrentProcess, OpenProcessToken};
    use windows::Win32::System::Shutdown::{
        ExitWindowsEx, EWX_FORCEIFHUNG, EWX_REBOOT, EWX_SHUTDOWN, EXIT_WINDOWS_FLAGS,
        SHUTDOWN_REASON,
    };

    pub fn shutdown() -> Result<(), OperationError> {
        exit_windows(EWX_SHUTDOWN)
    }

    pub fn reboot() -> Result<(), OperationError> {
        exit_windows(EWX_REBOOT)
    }

    fn exit_windows(flag: EXIT_WINDOWS_FLAGS) -> Result<(), OperationError> {
        enable_shutdown_privilege()?;
        // 交互式登录用户默认持有 SE_SHUTDOWN_NAME（仅处于禁用态），
        // 启用后即可调用，全程无需 UAC。
        unsafe { ExitWindowsEx(flag | EWX_FORCEIFHUNG, SHUTDOWN_REASON(0)) }
            .map_err(|e| OperationError::with_source("ExitWindowsEx failed", e))?;
        Ok(())
    }

    /// 启用当前进程令牌的 SeShutdownPrivilege。
    fn enable_shutdown_privilege() -> Result<(), OperationError> {
        unsafe {
            let mut token = HANDLE::default();
            OpenProcessToken(
                GetCurrentProcess(),
                TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY,
                &mut token,
            )
            .map_err(|e| OperationError::with_source("OpenProcessToken failed", e))?;

            let result = (|| -> Result<(), OperationError> {
                let mut luid = LUID::default();
                // SE_SHUTDOWN_NAME 本身就是 PCWSTR 常量（"SeShutdownPrivilege"）
                LookupPrivilegeValueW(None, SE_SHUTDOWN_NAME, &mut luid)
                    .map_err(|e| OperationError::with_source("LookupPrivilegeValueW failed", e))?;

                let tp = TOKEN_PRIVILEGES {
                    PrivilegeCount: 1,
                    Privileges: [LUID_AND_ATTRIBUTES {
                        Luid: luid,
                        Attributes: SE_PRIVILEGE_ENABLED,
                    }],
                };
                AdjustTokenPrivileges(token, false, Some(&tp as *const _), 0, None, None)
                    .map_err(|e| OperationError::with_source("AdjustTokenPrivileges failed", e))?;

                // API 返回成功不代表提权成功：ERROR_NOT_ALL_ASSIGNED 表示令牌未持有该权限
                if GetLastError() == ERROR_NOT_ALL_ASSIGNED {
                    return Err(OperationError::new(
                        "current process does not hold SeShutdownPrivilege",
                    ));
                }
                Ok(())
            })();

            let _ = CloseHandle(token);
            result
        }
    }
}

// ---------------------------------------------------------------------------
// Linux：通过 systemctl 走 systemd-logind + polkit。
// 活跃本地桌面会话默认免密放行（allow_active=yes），无需 root。
// 对应 Java 版 LinuxOperator。
// ---------------------------------------------------------------------------
#[cfg(target_os = "linux")]
mod linux_impl {
    use super::OperationError;
    use std::process::Command;

    pub fn shutdown() -> Result<(), OperationError> {
        run(&["poweroff"])
    }

    pub fn reboot() -> Result<(), OperationError> {
        run(&["reboot"])
    }

    fn run(args: &[&str]) -> Result<(), OperationError> {
        let output = Command::new("systemctl")
            .args(args)
            .output()
            .map_err(|e| OperationError::with_source("无法执行 systemctl，系统可能未使用 systemd", e))?;
        if output.status.success() {
            return Ok(());
        }
        Err(OperationError::new(format!(
            "systemctl {} exited with code {:?}（SSH 等非本地会话可能需要 polkit 授权）",
            args.join(" "),
            output.status.code()
        )))
    }
}

// ---------------------------------------------------------------------------
// macOS：通过 AppleScript 请求 System Events，图形会话下无需 root。
// 对应 Java 版 MacOperator。
// ---------------------------------------------------------------------------
#[cfg(target_os = "macos")]
mod macos_impl {
    use super::OperationError;
    use std::process::Command;

    pub fn shutdown() -> Result<(), OperationError> {
        run("shut down")
    }

    pub fn reboot() -> Result<(), OperationError> {
        run("restart")
    }

    fn run(action: &str) -> Result<(), OperationError> {
        Command::new("osascript")
            .args(["-e", &format!("tell application \"System Events\" to {action}")])
            .spawn()
            .map_err(|e| OperationError::with_source("无法执行 osascript", e))?;
        Ok(())
    }
}
