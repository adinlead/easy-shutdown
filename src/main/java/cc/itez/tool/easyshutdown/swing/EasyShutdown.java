package cc.itez.tool.easyshutdown.swing;

import cc.itez.tool.easyshutdown.Logger;
import cc.itez.tool.easyshutdown.TimeInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

public class EasyShutdown {
    private static JFrame primaryStage;

    public static JFrame getPrimaryStage() {
        return primaryStage;
    }

    private javax.swing.Timer timeline; // 将 Timeline 定义为类的成员变量
    private AtomicReference<String> operate = new AtomicReference<>(Operate.SHUTDOWN);

    public EasyShutdown() {
        // 创建JFrame
        primaryStage = new JFrame("易关机");
        primaryStage.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        primaryStage.setSize(500, 200);
        primaryStage.setResizable(false);
        // 设置窗口位于屏幕中央
        primaryStage.setLocationRelativeTo(null);

        // 设置窗口图标
        URL iconURL = getClass().getResource("/img/logo-32.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            primaryStage.setIconImage(icon.getImage());
        } else {
            Logger.warn("Icon not found: /img/logo-32.png");
        }

        // 创建JPanel
        JPanel vbox = new JPanel();
        vbox.setLayout(new BoxLayout(vbox, BoxLayout.Y_AXIS));
        vbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        vbox.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));

        // 创建一个JSlider对象，其值范围从0到150，初始值为0
        JSlider timeSlider = new JSlider(0, 150, 0);

        // 设置JSlider显示刻度标签
        timeSlider.setPaintLabels(true);

        // 设置JSlider的标签格式转换器
        // 用于将JSlider的值转换为字符串，并显示在标签上
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("现在"));
        labelTable.put(30, new JLabel("30分钟"));
        labelTable.put(60, new JLabel("1小时"));
        labelTable.put(84, new JLabel("3小时"));
        labelTable.put(102, new JLabel("6小时"));
        labelTable.put(126, new JLabel("12小时"));
        labelTable.put(150, new JLabel("24小时"));

        timeSlider.setLabelTable(labelTable);
        timeSlider.setMajorTickSpacing(6);
        timeSlider.setMinorTickSpacing(1);
        timeSlider.setPaintTicks(true);
        timeSlider.setSnapToTicks(true);

        vbox.add(timeSlider);

        // 创建JPanel
        JPanel bottomHbox = new JPanel();
        vbox.add(bottomHbox);
        bottomHbox.setLayout(new BoxLayout(bottomHbox, BoxLayout.X_AXIS));
        bottomHbox.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 创建JLabel
        JLabel timeShow = new JLabel("00:00:00");
        URL numberFontResource = getClass().getResource("/fonts/number-default.ttf");
        if (numberFontResource == null) {
            timeShow.setFont(new Font("Serif", Font.BOLD, 64));
        } else {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, numberFontResource.openStream());
                timeShow.setFont(font.deriveFont(72f));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        bottomHbox.add(timeShow);

        // 创建JPanel和JRadioButton
        JPanel actionBtnBox = new JPanel();
        bottomHbox.add(actionBtnBox);
        actionBtnBox.setLayout(new BoxLayout(actionBtnBox, BoxLayout.Y_AXIS));

        JRadioButton shutdownRadioButton = new JRadioButton("关机");
        shutdownRadioButton.setSelected(true);
        shutdownRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (shutdownRadioButton.isSelected()) {
                    Logger.debug("shutdownRadioButton ACTION");
                    operate.set(Operate.SHUTDOWN);
                }
            }
        });
        actionBtnBox.add(shutdownRadioButton);

        JRadioButton restartRadioButton = new JRadioButton("重启");
        restartRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (restartRadioButton.isSelected()) {
                    Logger.debug("restartRadioButton ACTION");
                    operate.set(Operate.REBOOT);
                }
            }
        });
        actionBtnBox.add(restartRadioButton);

        JRadioButton ringingRadioButton = new JRadioButton("响铃");
        ringingRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ringingRadioButton.isSelected()) {
                    Logger.debug("ringingRadioButton ACTION");
                    operate.set(Operate.RINGING);
                }
            }
        });
        actionBtnBox.add(ringingRadioButton);

        ButtonGroup actionType = new ButtonGroup();
        actionType.add(shutdownRadioButton);
        actionType.add(restartRadioButton);
        actionType.add(ringingRadioButton);

        // 设置Scene和设置Stage
        primaryStage.add(vbox);
        primaryStage.setVisible(true);

        this.bindSliderEvent(timeSlider, timeShow);
    }

    private void bindSliderEvent(JSlider timeSlider, JLabel timeShow) {
        AtomicReference<Integer> lastValue = new AtomicReference<>(0);
        // 添加数值变动监听器
        timeSlider.addChangeListener(e -> {
            int value = timeSlider.getValue();
            if (Math.abs(lastValue.get() - value) > 0.5) {
                lastValue.set(value);
                TimeInfo info = new TimeInfo(value);
                timeShow.setText(info.number());
                Logger.debug("timeSliderValue     >>>> " + value);
            }
        });
        timeSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                TimeInfo info = new TimeInfo(timeSlider.getValue());
                if (info.second() > 0) {
                    // 从info.second开始倒计时，并且每秒更新timeShow的text内容为this.formatterSecond(info.second)
                    startCountdown(info, timeShow);
                }
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (timeline != null) {
                    timeline.stop();
                }
            }
        });
    }

    private void startCountdown(TimeInfo info, JLabel timeShow) {
        // 停止之前的 Timeline
        if (timeline != null) {
            timeline.stop();
        }

        // 创建一个新的 Timeline 对象，每秒执行一次
        timeline = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 更新 timeShow 的文本内容
                timeShow.setText(info.secondDecrement().number());

                // 如果倒计时结束，停止 Timeline
                if (info.second() <= 0) {
                    timeline.stop();
                    // 可以在这里添加倒计时结束后的操作，例如执行关机、重启等
                    Logger.debug("operate >>>> " + operate.get());
                    Operate.execute(operate.get());
                }
            }
        });

        // 启动 Timeline
        timeline.start();
    }
}
