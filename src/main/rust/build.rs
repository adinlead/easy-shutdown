//! 构建脚本：把 `resources/img/logo/logo.ico` 编译进可执行文件，
//! 让资源管理器/任务栏显示应用图标。
//!
//! Windows 的 exe 图标存放在 PE 的 `.rsrc` 段，由资源脚本（.rc）+ 图标文件生成。
//! 这里在构建时用 mingw 的 `windres` 把 .rc 编译成 COFF 对象，再交给链接器。
//! 非 Windows 目标直接跳过；找不到 windres 时只告警，不中断构建。

use std::env;
use std::path::PathBuf;
use std::process::Command;

fn main() {
    let target_os = env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
    if target_os != "windows" {
        return;
    }

    let manifest = PathBuf::from(env::var("CARGO_MANIFEST_DIR").expect("CARGO_MANIFEST_DIR"));
    let out_dir = PathBuf::from(env::var("OUT_DIR").expect("OUT_DIR"));

    let ico = match manifest
        .join("../resources/img/logo/logo.ico")
        .canonicalize()
    {
        Ok(p) => p,
        Err(_) => {
            println!("cargo:warning=未找到 logo.ico，跳过图标嵌入");
            return;
        }
    };
    println!("cargo:rerun-if-changed={}", ico.display());

    // windres 对反斜杠敏感，路径统一转成正斜杠
    let ico_path = ico.display().to_string().replace('\\', "/");
    let rc_path = out_dir.join("app.rc");
    let res_path = out_dir.join("app.res");

    // 1 号图标 = exe 默认图标；VERSIONINFO 让文件属性里显示产品名
    let rc = format!(
        r#"1 ICON "{ico_path}"

1 VERSIONINFO
FILEVERSION 0,1,0,0
PRODUCTVERSION 0,1,0,0
FILEOS 0x40004
FILETYPE 0x1
BEGIN
  BLOCK "StringFileInfo"
  BEGIN
    BLOCK "080404b0"
    BEGIN
      VALUE "FileDescription", "Easy Shutdown"
      VALUE "ProductName", "Easy Shutdown"
      VALUE "FileVersion", "0.1.0"
      VALUE "ProductVersion", "0.1.0"
      VALUE "OriginalFilename", "easy-shutdown.exe"
    END
  END
  BLOCK "VarFileInfo"
  BEGIN
    VALUE "Translation", 0x804, 1200
  END
END
"#
    );

    if let Err(e) = std::fs::write(&rc_path, rc) {
        println!("cargo:warning=写入 app.rc 失败: {e}");
        return;
    }

    let status = Command::new("windres")
        .arg("--input")
        .arg(&rc_path)
        .arg("--output")
        .arg(&res_path)
        .arg("--output-format=coff")
        .status();

    match status {
        Ok(s) if s.success() => {
            println!("cargo:rustc-link-arg-bins={}", res_path.display());
        }
        _ => println!("cargo:warning=windres 编译资源失败，exe 将不带图标（请确认 mingw-w64 在 PATH 中）"),
    }
}
