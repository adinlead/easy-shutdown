package cc.itez.tool.easyshutdown.swing;

import cc.itez.tool.easyshutdown.Logger;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

public class Operate {
    public static final String SHUTDOWN = "shutdown";

    public static final String REBOOT = "reboot";
    public static final String RINGING = "ringing";

    private Player player;

    private String action;
    private final JFrame mainFrame;

    public Operate(JFrame mainFrame, String action) {
        this.mainFrame = mainFrame;
        this.action = action;
    }

    public void action(String action) {
        Logger.debug("Set operate action => {}", Operate.REBOOT);
        this.action = action;
    }

    public String action() {
        return action;
    }

    public void execute() {
        switch (this.action()) {
            case SHUTDOWN -> shutdown();
            case REBOOT -> reboot();
            case RINGING -> ringing();
            default -> Logger.warn("Unsupported operating system.");
        }
    }

    public void shutdown() {
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

    public void reboot() {
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

    public void ringing() {
        playAudio(Resource.SOUND_DEFAULT.path());
        showPopup();
    }

    private void playAudio(String filePath) {
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

    private void stopAudio() {
        if (player != null) {
            player.close();
        }
    }

    private void showPopup() {
        SwingUtilities.invokeLater(() -> {
            JDialog popupStage = new JDialog(mainFrame, "倒计时已结束", Dialog.ModalityType.APPLICATION_MODAL);
            popupStage.setUndecorated(true); // 去掉状态栏
            popupStage.setAlwaysOnTop(true); // 弹窗置于系统最顶层

            // 创建提示标签并设置样式
            JLabel label = new JLabel("倒计时已结束", SwingConstants.CENTER);
            label.setFont(new Font("Serif", Font.BOLD, 20));

            // 创建关闭按钮并设置样式
            JButton closeButton = new JButton("停止");
            closeButton.setFont(new Font("Serif", Font.BOLD, 20)); // 设置按钮字体大小为20

            // 使用 JPanel 布局并将内容居中
            JPanel layout = new JPanel(new GridBagLayout());
            layout.setBackground(Color.WHITE);
            layout.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK, 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(10, 10, 10, 10);

            // 添加标签
            layout.add(label, gbc);

            // 添加按钮
            gbc.gridy = 1;
            layout.add(closeButton, gbc);

            closeButton.addActionListener(e -> {
                stopAudio();
                popupStage.dispose();
                // 将主UI设为活动层
                mainFrame.toFront();
            });

            // 设置场景并确保窗口大小刚好容纳内容
            popupStage.add(layout);
            popupStage.pack();
            popupStage.setLocationRelativeTo(mainFrame);
            popupStage.setVisible(true);
        });
    }
}
