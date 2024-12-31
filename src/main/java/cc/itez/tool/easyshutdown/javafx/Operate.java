package cc.itez.tool.easyshutdown.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.StageStyle;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

public class Operate {
    public static final String SHUTDOWN = "shutdown";

    public static final String REBOOT = "reboot";
    public static final String RINGING = "ringing";

    private static Player player;

    public static void execute(String operateType) {
        switch (operateType) {
            case SHUTDOWN -> shutdown();
            case REBOOT -> reboot();
            case RINGING -> ringing();
            default -> Logger.warn("Unsupported operating system.");
        }
    }

    public static void shutdown() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec("shutdown -s -t 0");
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                Runtime.getRuntime().exec("shutdown -h now");
            } else {
                Logger.warn("Unsupported operating system.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reboot() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec("shutdown -r -t 0");
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                Runtime.getRuntime().exec("reboot");
            } else {
                Logger.warn("Unsupported operating system.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void ringing() {
        playAudio("/sounds/morning-joy.mp3");
        showPopup();
    }

    private static void playAudio(String filePath) {
        try {
            URL resource = Operate.class.getResource(filePath);
            assert resource != null;
            BufferedInputStream bufferedInputStream = new BufferedInputStream(resource.openStream());
            player = new Player(bufferedInputStream);
            new Thread(() -> {
                try {
                    player.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }

    private static void stopAudio() {
        if (player != null) {
            player.close();
        }
    }

    private static void showPopup() {
        Platform.runLater(() -> {
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.UNDECORATED); // 去掉状态栏
            popupStage.setAlwaysOnTop(true); // 弹窗置于系统最顶层

            // 创建提示标签并设置样式
            Label label = new Label("倒计时已结束");
            label.setFont(Font.font(20));
            label.setAlignment(Pos.CENTER);

            // 创建关闭按钮并设置样式
            Button closeButton = new Button("停止");
            closeButton.setFont(Font.font(20)); // 设置按钮字体大小为20
            closeButton.setMaxWidth(Double.MAX_VALUE); // 按钮宽度自适应内容
            closeButton.setOnAction(event -> {
                stopAudio();
                popupStage.close();
                // 将主UI设为活动层
                Platform.runLater(() -> {
                    if (EasyShutdown.getPrimaryStage() != null) {
                        EasyShutdown.getPrimaryStage().toFront();
                    }
                });
            });

            // 使用 VBox 布局并将内容居中
            VBox layout = new VBox(10);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));
            layout.getChildren().addAll(label, closeButton);

            // 设置边框和阴影效果
            layout.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2;");
            DropShadow shadow = new DropShadow();
            shadow.setOffsetX(5);
            shadow.setOffsetY(5);
            shadow.setColor(Color.BLACK);
            layout.setEffect(shadow);

            // 设置场景并确保窗口大小刚好容纳内容
            Scene scene = new Scene(layout);
            popupStage.setScene(scene);
            // 确保窗口大小刚好容纳内容
            popupStage.sizeToScene();
            popupStage.show();
        });
    }
}
