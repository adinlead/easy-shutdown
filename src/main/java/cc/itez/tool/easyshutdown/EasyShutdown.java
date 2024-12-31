package cc.itez.tool.easyshutdown;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

public class EasyShutdown extends Application {
    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private Timeline timeline; // 将 Timeline 定义为类的成员变量
    private AtomicReference<String> operate = new AtomicReference<>(Operate.SHUTDOWN);

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
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
        URL numberFontResource = getClass().getResource("/fonts/cursed-timer-ulil-font/CursedTimerUlil-Aznm.ttf");
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
                System.out.println("shutdownRadioButton ACTION");
                operate.set(Operate.SHUTDOWN);
            }
        });
        actionBtnBox.getChildren().add(shutdownRadioButton);

        RadioButton restartRadioButton = new RadioButton("重启");
        restartRadioButton.setToggleGroup(actionType);
        restartRadioButton.setPadding(new Insets(0, 10, 10, 10));
        restartRadioButton.addEventHandler(ActionEvent.ACTION, event -> {
            if (restartRadioButton.isSelected()) {
                System.out.println("restartRadioButton ACTION");
                operate.set(Operate.REBOOT);
            }
        });
        actionBtnBox.getChildren().add(restartRadioButton);

        RadioButton ringingRadioButton = new RadioButton("响铃");
        ringingRadioButton.setToggleGroup(actionType);
        ringingRadioButton.setPadding(new Insets(0, 10, 10, 10));
        ringingRadioButton.addEventHandler(ActionEvent.ACTION, event -> {
            if (ringingRadioButton.isSelected()) {
                System.out.println("ringingRadioButton ACTION");
                operate.set(Operate.RINGING);
            }
        });
        actionBtnBox.getChildren().add(ringingRadioButton);


        // 创建Scene和设置Stage
        Scene scene = new Scene(vbox, 500, 200);
        stage.setTitle("定时关机");
        stage.setScene(scene);
        stage.setResizable(false);
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
                TimeInfo info = this.toTimeInfo(value);
                timeShow.setText(info.number);
                System.out.println("timeSliderValue     >>>> " + value);
            }
        });
        timeSlider.valueChangingProperty().addListener((observableValue, isRelease, isCapture) -> {
            TimeInfo info = this.toTimeInfo(timeSlider.getValue());
            if (isRelease) {
                // 从info.second开始倒计时，并且每秒更新timeShow的text内容为this.formatterSecond(info.second)
                startCountdown(info, timeShow);
            } else if (isCapture) {
                if (timeline != null) {
                    timeline.stop();
                }
            }
            System.out.println(" ===== valueChangingProperty ===== ");
            System.out.println("observableValue     >>>> " + observableValue);
            System.out.println("timeSliderValue     >>>> " + timeSlider.getValue());
            System.out.println("infoNumber          >>>> " + info.number);
            System.out.println("isRelease           >>>> " + isRelease);
            System.out.println("isCapture           >>>> " + isCapture);
        });
    }

    private void startCountdown(TimeInfo info, Label timeShow) {
        // 停止之前的 Timeline
        if (timeline != null) {
            timeline.stop();
        }

        // 创建一个新的 Timeline 对象，每秒执行一次
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // 每秒减少一秒
            info.second--;
            // 更新 timeShow 的文本内容
            timeShow.setText(this.formatterSecond(info.second));

            // 如果倒计时结束，停止 Timeline
            if (info.second <= 0) {
                timeline.stop();
                // 可以在这里添加倒计时结束后的操作，例如执行关机、重启等
                System.out.println("operate >>>> " + operate.get());
                Operate.execute(operate.get());
            }
        }));

        // 设置 Timeline 的循环次数为无限次
        timeline.setCycleCount(Timeline.INDEFINITE);
        // 启动 Timeline
        timeline.play();
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
     * @return
     */
    private TimeInfo toTimeInfo(double value) {
        int val = (int) value;
        TimeInfo info = new TimeInfo();
        info.pointer = value;
        if (val <= 60) {
            info.second = val * 60;
            info.number = this.formatterSecond(info.second);
        } else if (val <= 84) {
            int nv = val - 60;
            info.second = 3600 + nv * 5 * 60;
            info.number = this.formatterSecond(info.second);
        } else if (val <= 102) {
            int nv = val - 84;
            info.second = (3 * 3600) + nv * 10 * 60;
            info.number = this.formatterSecond(info.second);
        } else if (val <= 126) {
            int nv = val - 102;
            info.second = (6 * 3600) + nv * 15 * 60;
            info.number = this.formatterSecond(info.second);
        } else if (val <= 150) {
            int nv = val - 126;
            info.second = (12 * 3600) + nv * 30 * 60;
            info.number = this.formatterSecond(info.second);
        }
        return info;
    }

    private String formatterSecond(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private String convertValueToNonLinear(double value) {
        return String.format("%.2f", value);
    }

    public static void main(String[] args) {
        launch();
    }
}

