package cc.itez.tool.easyshutdown.javafx;

import cc.itez.tool.easyshutdown.Logger;
import cc.itez.tool.easyshutdown.TimeInfo;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class EasyShutdown extends Application {
    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private Timeline timeline;
    private final AtomicReference<String> operate = new AtomicReference<>(Operate.SHUTDOWN);

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        // 创建VBox
        VBox vbox = new VBox(20.0);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        vbox.setPadding(new Insets(40.0, 20.0, 20.0, 20.0));

        // 创建一个Slider对象，其值范围从0到150，初始值为0
        // 创建Slider
        Slider timeSlider = new Slider(0, 150, 0);

        // 设置Slider显示刻度标签
        timeSlider.setShowTickLabels(true);

        // 设置Slider的标签格式转换器
        // 用于将Slider的值转换为字符串，并显示在标签上
        timeSlider.labelFormatterProperty().set(new StringConverter<>() {
            @Override
            public String toString(Double aDouble) {
                // 使用switch表达式根据Slider的值返回对应的字符串标签
                return switch (aDouble.intValue()) {
                    case 0 -> "现在";
                    case 30 -> "30分钟";
                    case 60 -> "1小时";
                    case 84 -> "3小时";
                    case 102 -> "6小时";
                    case 126 -> "12小时";
                    case 150 -> "24小时";
                    default -> null;
                };
            }

            @Override
            public Double fromString(String s) {
                // 此方法用于将字符串转换为Slider的值，但这里始终返回0D，因为转换逻辑并未实现
                return 0D;
            }
        });

        // 设置Slider显示刻度标记
        timeSlider.setShowTickMarks(true);

        // 设置Slider的滑块在移动时只能停留在刻度上
        timeSlider.setSnapToTicks(true);

        // 设置Slider每小格（次刻度）的数量
        timeSlider.setMinorTickCount(5);

        // 设置Slider每次点击滑块时移动的值
        timeSlider.setBlockIncrement(1D);

        // 设置Slider的主要刻度单元，即每隔多少个单位显示一个主要刻度
        // 这里设置为6，意味着每隔6个单位（比如0, 6, 12, 18, ...）显示一个主要刻度
        timeSlider.setMajorTickUnit(6);

        vbox.getChildren().add(timeSlider);

        // 创建ToggleGroup
        ToggleGroup actionType = new ToggleGroup();

        HBox bottomHbox = new HBox();
        vbox.getChildren().add(bottomHbox);
        bottomHbox.setPrefWidth(200.0);
        bottomHbox.setAlignment(Pos.CENTER);

        // 创建Label
        Label timeShow = new Label("00:00:00");
        URL numberFontResource = getClass().getResource("/fonts/number-default.ttf");
        if (numberFontResource == null) {
            timeShow.setFont(new Font(64));
        } else {
            timeShow.setFont(Font.loadFont(numberFontResource.toExternalForm(), 72));
        }
        bottomHbox.getChildren().add(timeShow);
        // 创建HBox和RadioButton
        VBox actionBtnBox = new VBox();
        bottomHbox.getChildren().add(actionBtnBox);

        RadioButton shutdownRadioButton = new RadioButton("关机");
        shutdownRadioButton.setToggleGroup(actionType);
        shutdownRadioButton.setSelected(true);
        shutdownRadioButton.setPadding(new Insets(0, 10, 10, 10));
        shutdownRadioButton.addEventHandler(ActionEvent.ACTION, event -> {
            if (shutdownRadioButton.isSelected()) {
                Logger.debug("shutdownRadioButton ACTION");
                operate.set(Operate.SHUTDOWN);
            }
        });
        actionBtnBox.getChildren().add(shutdownRadioButton);

        RadioButton restartRadioButton = new RadioButton("重启");
        restartRadioButton.setToggleGroup(actionType);
        restartRadioButton.setPadding(new Insets(0, 10, 10, 10));
        restartRadioButton.addEventHandler(ActionEvent.ACTION, event -> {
            if (restartRadioButton.isSelected()) {
                Logger.debug("restartRadioButton ACTION");
                operate.set(Operate.REBOOT);
            }
        });
        actionBtnBox.getChildren().add(restartRadioButton);

        RadioButton ringingRadioButton = new RadioButton("响铃");
        ringingRadioButton.setToggleGroup(actionType);
        ringingRadioButton.setPadding(new Insets(0, 10, 10, 10));
        ringingRadioButton.addEventHandler(ActionEvent.ACTION, event -> {
            if (ringingRadioButton.isSelected()) {
                Logger.debug("ringingRadioButton ACTION");
                operate.set(Operate.RINGING);
            }
        });
        actionBtnBox.getChildren().add(ringingRadioButton);


        // 创建Scene和设置Stage
        Scene scene = new Scene(vbox, 500, 200);
        stage.setTitle("易关机");
        stage.setScene(scene);
        stage.setResizable(false);

        // 设置窗口图标
        URL iconURL = getClass().getResource("/img/logo/logo-32.png");
        if (iconURL != null) {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(iconURL.toExternalForm());
            primaryStage.getIcons().add(icon);
        } else {
            Logger.warn("Icon not found: /img/logo-32.png");
        }

        stage.show();

        this.bindSliderEvent(timeSlider, timeShow);
    }

    private void bindSliderEvent(Slider timeSlider, Label timeShow) {
        AtomicReference<Double> lastValue = new AtomicReference<>(0D);
        // 添加数值变动监听器
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double value = timeSlider.getValue();
            if (Math.abs(lastValue.get() - value) > 0.5) {
                lastValue.set(value);
                TimeInfo info = new TimeInfo(value);
                timeShow.setText(info.number());
                Logger.debug("timeSliderValue     >>>> " + value);
            }
        });
        timeSlider.valueChangingProperty().addListener((observableValue, isRelease, isCapture) -> {
            TimeInfo info = new TimeInfo(timeSlider.getValue());
            if (info.second() > 0 && isRelease) {
                // 从info.second()开始倒计时，并且每秒更新timeShow的text内容为this.formatterSecond(info.second())
                startCountdown(info, timeShow);
            } else if (isCapture) {
                if (timeline != null) {
                    timeline.stop();
                }
            }
            Logger.debug(" ===== valueChangingProperty ===== ");
            Logger.debug("observableValue     >>>> " + observableValue);
            Logger.debug("timeSliderValue     >>>> " + timeSlider.getValue());
            Logger.debug("infoNumber          >>>> " + info.number());
            Logger.debug("isRelease           >>>> " + isRelease);
            Logger.debug("isCapture           >>>> " + isCapture);
        });
    }

    private void startCountdown(TimeInfo info, Label timeShow) {
        // 停止之前的 Timeline
        if (timeline != null) {
            timeline.stop();
        }

        // 创建一个新的 Timeline 对象，每秒执行一次
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // 更新 timeShow 的文本内容
            timeShow.setText(info.secondDecrement().number());

            // 如果倒计时结束，停止 Timeline
            if (info.second() <= 0) {
                timeline.stop();
                // 可以在这里添加倒计时结束后的操作，例如执行关机、重启等
                Logger.debug("operate >>>> " + operate.get());
                Operate.execute(operate.get());
            }
        }));

        // 设置 Timeline 的循环次数为无限次
        timeline.setCycleCount(Timeline.INDEFINITE);
        // 启动 Timeline
        timeline.play();
    }
}

