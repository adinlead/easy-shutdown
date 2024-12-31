package main

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
)

func main() {
	myApp := app.New()
	window := myApp.NewWindow("定时关机")
	
	shutdown := NewEasyShutdown()
	window.SetContent(shutdown.BuildUI())
	
	window.Resize(fyne.NewSize(500, 200))
	window.SetFixedSize(true)
	window.ShowAndRun()
}