package main

import (
	"fmt"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

type EasyShutdown struct {
	timeLabel    *widget.Label
	slider       *widget.Slider
	operateType  string
	timer        *time.Timer
	currentValue float64
}

func NewEasyShutdown() *EasyShutdown {
	return &EasyShutdown{
		operateType: "shutdown",
	}
}

func (e *EasyShutdown) BuildUI() fyne.CanvasObject {
	// 创建时间显示标签
	e.timeLabel = widget.NewLabel("00:00:00")
	e.timeLabel.TextStyle = fyne.TextStyle{Monospace: true}
	e.timeLabel.TextSize = 32

	// 创建滑块
	e.slider = widget.NewSlider(0, 150)
	e.slider.Step = 1
	e.slider.OnChanged = e.onSliderChanged

	// 创建滑块刻度标签容器
	tickLabels := container.NewGridWithColumns(7,
		widget.NewLabel("现在"),
		widget.NewLabel("30分钟"),
		widget.NewLabel("1小时"),
		widget.NewLabel("3小时"),
		widget.NewLabel("6小时"),
		widget.NewLabel("12小时"),
		widget.NewLabel("24小时"),
	)

	// 设置标签样式为居中
	for _, label := range tickLabels.Objects {
		if l, ok := label.(*widget.Label); ok {
			l.Alignment = fyne.TextAlignCenter
		}
	}

	// 创建单选按钮组
	radioGroup := widget.NewRadioGroup([]string{"关机", "重启", "响铃"}, func(value string) {
		switch value {
		case "关机":
			e.operateType = "shutdown"
		case "重启":
			e.operateType = "reboot"
		case "响铃":
			e.operateType = "ringing"
		}
	})
	radioGroup.SetSelected("关机")
	radioGroup.Horizontal = true

	// 布局
	content := container.NewVBox(
		e.slider,
		tickLabels,
		container.NewPadded(e.timeLabel),
		radioGroup,
	)

	return content
}

func (e *EasyShutdown) onSliderChanged(value float64) {
	if e.timer != nil {
		e.timer.Stop()
	}

	// 对齐到最近的刻度
	alignedValue := e.alignToNearestTick(value)
	if alignedValue != value {
		e.slider.Value = alignedValue
		return
	}

	seconds := e.calculateSeconds(alignedValue)
	e.timeLabel.SetText(formatSeconds(seconds))
	
	if seconds > 0 {
		e.startCountdown(seconds)
	}
}

/**
  * | 时间(小时) | 跨度(分钟) | 步长(分钟) | 刻度数 | 刻度位置 |
  * |--------|--------|--------|-----|------|
  * | 1      | 60     | 1      | 60  | 60   |
  * | 3      | 120    | 5      | 24  | 84   |
  * | 6      | 180    | 10     | 18  | 102  |
  * | 12     | 360    | 15     | 24  | 126  |
  * | 24     | 720    | 30     | 24  | 150  |
  *
  * @param value
  */
// 新增：将滑块值对齐到最近的刻度
func (e *EasyShutdown) alignToNearestTick(value float64) float64 {
	// 主要刻度点
	ticks := []float64{0, 30, 60, 84, 102, 126, 150}
	
	// 找到最近的刻度点
	nearest := ticks[0]
	minDiff := float64(1000)
	
	for _, tick := range ticks {
		diff := abs(value - tick)
		if diff < minDiff {
			minDiff = diff
			nearest = tick
		}
	}
	
	// 如果在主要刻度点之间，使用相应的步长
	if value <= 60 {
		// 1分钟步长
		return float64(int(value + 0.5))
	} else if value <= 84 {
		// 5分钟步长
		return 60 + float64(int((value-60+2.5)/5)*5)
	} else if value <= 102 {
		// 10分钟步长
		return 84 + float64(int((value-84+5)/10)*10)
	} else if value <= 126 {
		// 15分钟步长
		return 102 + float64(int((value-102+7.5)/15)*15)
	} else {
		// 30分钟步长
		return 126 + float64(int((value-126+15)/30)*30)
	}
}

// 新增：辅助函数
func abs(x float64) float64 {
	if x < 0 {
		return -x
	}
	return x
}

func formatSeconds(seconds int) string {
	hours := seconds / 3600
	minutes := (seconds % 3600) / 60
	secs := seconds % 60
	return fmt.Sprintf("%02d:%02d:%02d", hours, minutes, secs)
}

func (e *EasyShutdown) startCountdown(seconds int) {
	e.timer = time.NewTimer(time.Duration(seconds) * time.Second)
	
	go func() {
		remaining := seconds
		ticker := time.NewTicker(time.Second)
		defer ticker.Stop()

		for remaining > 0 {
			select {
			case <-ticker.C:
				remaining--
				e.timeLabel.SetText(formatSeconds(remaining))
			case <-e.timer.C:
				e.executeOperation()
				return
			}
		}
	}()
}

func (e *EasyShutdown) executeOperation() {
	switch e.operateType {
	case "shutdown":
		executeShutdown()
	case "reboot":
		executeReboot()
	case "ringing":
		showRingingDialog()
	}
}