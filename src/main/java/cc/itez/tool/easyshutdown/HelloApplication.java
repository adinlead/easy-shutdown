package cc.itez.tool.easyshutdown;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 创建VBox
        VBox vbox = new VBox(20.0);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        vbox.setPadding(new Insets(40.0, 20.0, 20.0, 20.0));

        // 创建Label
        Label timeShow = new Label("00:00");
        timeShow.setFont(new Font(36.0));
        vbox.getChildren().add(timeShow);
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

        // 创建Button
        Button startButton = new Button("开始");
        startButton.setFont(new Font(24.0));
        vbox.getChildren().add(startButton);

        // 创建ToggleGroup
        ToggleGroup actionType = new ToggleGroup();

        // 创建HBox和RadioButton
        HBox hbox = new HBox();
        hbox.setPrefWidth(200.0);

        RadioButton shutdownRadioButton = new RadioButton("关机");
        shutdownRadioButton.setToggleGroup(actionType);
        shutdownRadioButton.setSelected(true);
        shutdownRadioButton.setPadding(new Insets(0, 0, 20, 10));
        hbox.getChildren().add(shutdownRadioButton);

        RadioButton restartRadioButton = new RadioButton("重启");
        restartRadioButton.setToggleGroup(actionType);
        restartRadioButton.setPadding(new Insets(0, 10, 20, 0));
        hbox.getChildren().add(restartRadioButton);

        vbox.getChildren().add(hbox);

        // 创建Scene和设置Stage
        Scene scene = new Scene(vbox, 500, 300);
        stage.setTitle("定时关机");
        stage.setScene(scene);
        stage.show();

        this.bindSliderEvent(timeSlider, timeShow);
    }

    private void bindSliderEvent(Slider timeSlider, Label timeShow) {
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            String nonLinearValue = convertValueToNonLinear(newValue.doubleValue());
            timeShow.setText(nonLinearValue);
        });
    }

    private TimeInfo toTimeInfo(int val) {
        TimeInfo info = new TimeInfo();
        if (val < 60) {
            info.number = String.format("00:%02d", val);
            info.second = val * 60L;
            info.pointer = (double) val;
        } else if (val == 60) {
            info.number = "01:00";
            info.second = val * 60L;
            info.pointer = (double) val;
        } else if (val < 84) {
            int nv = val - 60;
            info.number = String.format("00:%02d", val);
            info.second = 3600L + nv * 5 * 60L;
            info.pointer = (double) val;
        } else if (val == 84) {
            int nv = val - 60;
            info.number = String.format("00:%02d", val);
            info.second = 3600L + nv * 5 * 60L;
            info.pointer = (double) val;
        } else if (val < 102) {
        } else if (val == 102) {
        } else if (val < 126) {
        } else if (val == 126) {
        } else if (val < 150) {
        } else if (val == 150) {

        }
        return info;
    }

    private String convertValueToNonLinear(double value) {
        return String.format("%.2f", value);
    }

    public static void main(String[] args) {
        launch();
    }
}

class TimeInfo {
    String number;
    Long second;
    Double pointer;
}
