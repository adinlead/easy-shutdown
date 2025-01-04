package cc.itez.tool.easyshutdown.swing;

import cc.itez.tool.easyshutdown.Logger;
import cc.itez.tool.easyshutdown.TimeInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

public class EasyShutdown {
    private static JFrame primaryStage;
    private Point initialClick;

    public static JFrame getPrimaryStage() {
        return primaryStage;
    }

    private javax.swing.Timer timeline; // 将 Timeline 定义为类的成员变量
    private final AtomicReference<String> operate = new AtomicReference<>(Operate.SHUTDOWN);

    public EasyShutdown() {
        // 创建JFrame
        primaryStage = new JFrame("易关机");
        primaryStage.setSize(500, 200);
        primaryStage.setResizable(false);
        // 设置窗口位于屏幕中央
        primaryStage.setLocationRelativeTo(null);
        // 设置窗口为无边框
        primaryStage.setUndecorated(true);

        setCustomStatusBar();

        setMainPanel();

        // 设置窗口可见
        primaryStage.setVisible(true);
    }

    /**
     * 设置自定义状态栏
     */
    public void setCustomStatusBar() {
        // 创建并配置自定义状态栏
        JPanel customStatusBar = new JPanel();
        // 设置边框为1px黑色边框
        customStatusBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        // 设置背景颜色为白色
        customStatusBar.setBackground(Color.WHITE);
        // 添加鼠标监听器以实现拖拽功能
        customStatusBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        customStatusBar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // 计算新的窗口位置
                int thisX = primaryStage.getLocation().x;
                int thisY = primaryStage.getLocation().y;

                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                primaryStage.setLocation(X, Y);
            }
        });

        customStatusBar.setLayout(new BorderLayout());
        { // 创建并配置左侧部分(图标和标题)
            // 设置窗口图标
            JLabel iconLabel = new JLabel("易关机");
            iconLabel.setHorizontalAlignment(SwingConstants.LEFT);
            iconLabel.setFont(new Font("Serif", Font.BOLD, 16));
            // 设置按钮的背景为透明
            iconLabel.setOpaque(false);
            URL iconURL = getClass().getResource("/img/logo/logo-32.png");
            if (iconURL != null) {
                iconLabel.setIcon(new ImageIcon(new ImageIcon(iconURL).getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
            } else {
                Logger.warn("Icon not found: /img/logo-32.png");
            }

            // 创建按钮组，这个按钮组放到右边
            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.add(iconLabel);

            customStatusBar.add(titlePanel, BorderLayout.WEST);
        }
        { // 创建并配置右侧部分(按钮)
            // 创建按钮组，这个按钮组放到右边
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            // 按钮右对齐
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            // 添加托盘按钮
            buttonPanel.add(createActionBtn("/img/act_btn/tray.png", "托盘", e -> System.out.println("托盘功能")));
            // 添加设置按钮
            buttonPanel.add(createActionBtn("/img/act_btn/setting.png", "设置", e -> System.out.println("设置功能")));
            // 添加关闭按钮
            buttonPanel.add(createActionBtn("/img/act_btn/close.png", "关闭", e -> {
                Logger.info("即将退出程序");
                System.exit(0);
            }));

            // 将按钮加入到自定义状态栏
            customStatusBar.add(buttonPanel, BorderLayout.EAST);
        }
        primaryStage.add(customStatusBar, BorderLayout.NORTH);
    }

    /**
     * 设置主功能面板
     */
    public void setMainPanel() {
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
        shutdownRadioButton.addActionListener(e -> {
            if (shutdownRadioButton.isSelected()) {
                Logger.debug("shutdownRadioButton ACTION");
                operate.set(Operate.SHUTDOWN);
            }
        });
        actionBtnBox.add(shutdownRadioButton);

        JRadioButton restartRadioButton = new JRadioButton("重启");
        restartRadioButton.addActionListener(e -> {
            if (restartRadioButton.isSelected()) {
                Logger.debug("restartRadioButton ACTION");
                operate.set(Operate.REBOOT);
            }
        });
        actionBtnBox.add(restartRadioButton);

        JRadioButton ringingRadioButton = new JRadioButton("响铃");
        ringingRadioButton.addActionListener(e -> {
            if (ringingRadioButton.isSelected()) {
                Logger.debug("ringingRadioButton ACTION");
                operate.set(Operate.RINGING);
            }
        });
        actionBtnBox.add(ringingRadioButton);

        ButtonGroup actionType = new ButtonGroup();
        actionType.add(shutdownRadioButton);
        actionType.add(restartRadioButton);
        actionType.add(ringingRadioButton);

        // 设置Scene和设置Stage
        primaryStage.add(vbox);

        this.bindSliderEvent(timeSlider, timeShow);
    }

    private JButton createActionBtn(String iconPath, String title, ActionListener listener) {
        JButton button = new JButton();
        // 加载图片资源
        URL iconURL = getClass().getResource(iconPath);
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(new ImageIcon(iconURL).getImage()
                    .getScaledInstance(22, 22, Image.SCALE_SMOOTH));
            icon.setDescription(title);
            button.setIcon(icon);
            button.setToolTipText(title);
        } else {
            button.setText(title);
            button.setToolTipText(title);
            Logger.warn("Icon not found: " + iconPath);
        }

        // 设置按钮的背景为透明
        button.setContentAreaFilled(false);
        // 设置按钮的背景为透明
        button.setOpaque(false);

        // 设置按钮的边框为1px黑色边框
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

        // 添加鼠标监听器
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Color.GRAY);
                button.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(null);
                button.setOpaque(false);
            }
        });

        button.addActionListener(listener);
        return button;
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
        timeline = new javax.swing.Timer(1000, e -> {
            // 更新 timeShow 的文本内容
            timeShow.setText(info.secondDecrement().number());

            // 如果倒计时结束，停止 Timeline
            if (info.second() <= 0) {
                timeline.stop();
                // 可以在这里添加倒计时结束后的操作，例如执行关机、重启等
                Logger.debug("operate >>>> " + operate.get());
                Operate.execute(operate.get());
            }
        });

        // 启动 Timeline
        timeline.start();
    }
}
