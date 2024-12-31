package main

import (
	"os/exec"
	"runtime"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/dialog"
)

func executeShutdown() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "windows":
		cmd = exec.Command("shutdown", "-s", "-t", "0")
	case "linux", "darwin":
		cmd = exec.Command("shutdown", "-h", "now")
	}
	if cmd != nil {
		cmd.Run()
	}
}

func executeReboot() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "windows":
		cmd = exec.Command("shutdown", "-r", "-t", "0")
	case "linux":
		cmd = exec.Command("reboot")
	case "darwin":
		cmd = exec.Command("shutdown", "-r", "now")
	}
	if cmd != nil {
		cmd.Run()
	}
}

func showRingingDialog() {
	// 由于 Fyne 不直接支持音频播放，这里只显示一个对话框
	dialog.ShowCustom("倒计时结束", "确定",
		widget.NewLabel("时间到！"),
		fyne.CurrentApp().Driver().AllWindows()[0])
}