package cc.itez.tool.easyshutdown.swing;

import cc.itez.tool.easyshutdown.Logger;
import cc.itez.tool.easyshutdown.TimeInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

public class EasyShutdown {
    private final JFrame mainFrame = new JFrame();
    private Point initialClick;

    private javax.swing.Timer timeline; // 将 Timeline 定义为类的成员变量
    private final Operate operate = new Operate(mainFrame, Operate.SHUTDOWN);
    private final Setting setting = new Setting(mainFrame);

    private Font defaultTextFont;
    private Font defaultNumberFont;

    public EasyShutdown() {
        super();
        this.init();
    }

    public void init() {
        this.initVariables();
        // 创建主UI窗口
        mainFrame.setTitle("易关机");
        mainFrame.setSize(500, 200);
        mainFrame.setResizable(false);
        // 设置窗口位于屏幕中央
        mainFrame.setLocationRelativeTo(null);
        // 设置窗口图标
        mainFrame.setIconImage(Toolkit.getDefaultToolkit().createImage(Resource.IMG_LOGO_64.url()));
        // 设置窗口为无边框
        mainFrame.setUndecorated(true);
        // 设置自定义状态栏
        buildCustomTitleBar();
        // 设置主面板
        buildMainPanel();
        // 创建系统托盘图标
        buildSystemTray();
        // 设置窗口可见
        mainFrame.setVisible(true);
    }

    public void initVariables() {
        try (InputStream stream = Resource.FONT_TEXT_DEFAULT.stream()) {
            defaultTextFont = Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException(e);
        }
        try (InputStream stream = Resource.FONT_NUMBER_DEFAULT.stream()) {
            defaultNumberFont = Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建系统托盘图标和相关功能
     * 判断系统是否支持托盘功能，如果支持，则创建一个托盘图标。
     * 右击托盘图标时，显示一个弹出菜单，菜单中仅包含一个退出选项。
     * 双击托盘图标时，展示或隐藏主界面。
     */
    private void buildSystemTray() {
        // 判断系统是否支持托盘功能
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            // 创建一个托盘图标
            Image image = Toolkit.getDefaultToolkit().getImage(Resource.IMG_LOGO_32.url())
                    .getScaledInstance(16, 16, Image.SCALE_SMOOTH); // 调整图像大小
            PopupMenu popup = new PopupMenu();

            // 创建一个退出选项
            MenuItem exitItem = new MenuItem();
            exitItem.setLabel("退出");
            exitItem.setFont(defaultTextFont.deriveFont(16F));
            exitItem.addActionListener(e -> System.exit(0));

            popup.add(exitItem);

            // 创建托盘图标
            TrayIcon trayIcon = new TrayIcon(image, "易关机", popup);

            // 双击托盘图标时，展示或隐藏主界面
            trayIcon.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        mainFrame.setVisible(!mainFrame.isVisible());
                    }
                }
            });

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                Logger.error(e, "无法添加系统托盘图标: {}", e.getMessage());
            }
        } else {
            Logger.error("系统不支持托盘功能");
        }
    }

    /**
     * 构建自定义状态栏
     * 该方法用于创建和配置一个自定义的状态栏，包括设置其外观、布局以及拖拽功能
     */
    public void buildCustomTitleBar() {
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
                int thisX = mainFrame.getLocation().x;
                int thisY = mainFrame.getLocation().y;

                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                mainFrame.setLocation(X, Y);
            }
        });

        customStatusBar.setLayout(new BorderLayout());
        { // 创建并配置左侧部分(图标和标题)
            // 设置窗口图标
            JLabel iconLabel = createLabel("易关机", 16);
            iconLabel.setHorizontalAlignment(SwingConstants.LEFT);
            // 设置按钮的背景为透明
            iconLabel.setOpaque(false);
            iconLabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().createImage(Resource.IMG_LOGO_32.url())
                    .getScaledInstance(18, 18, Image.SCALE_SMOOTH)));

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
            buttonPanel.add(createActionButton(Resource.IMG_BAR_BTN_TRAY, "托盘", e -> System.out.println("托盘功能")));
            // 添加设置按钮
            buttonPanel.add(createActionButton(Resource.IMG_BAR_BTN_SETTING, "设置", e -> {
                Logger.info("即将进入设置");
                setting.popup();
            }));
            // 添加关闭按钮
            buttonPanel.add(createActionButton(Resource.IMG_BAR_BTN_CLOSE, "关闭", e -> {
                Logger.info("即将退出程序");
                System.exit(0);
            }));

            // 将按钮加入到自定义状态栏
            customStatusBar.add(buttonPanel, BorderLayout.EAST);
        }
        mainFrame.add(customStatusBar, BorderLayout.NORTH);
    }

    /**
     * 创建一个带有图标和标题的动作按钮
     *
     * @param resource 图标的枚举，用于从类路径中加载图标
     * @param title    按钮的标题，用作工具提示和备用文本
     * @param listener 按钮的动作事件监听器
     * @return 返回一个配置好的JButton对象
     */
    private JButton createActionButton(Resource resource, String title, ActionListener listener) {
        JButton button = new JButton();
        // 加载图片资源
        ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(resource.url())
                .getScaledInstance(22, 22, Image.SCALE_SMOOTH));
        icon.setDescription(title);
        button.setIcon(icon);
        button.setToolTipText(title);

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

    /**
     * 设置主功能面板
     */
    public void buildMainPanel() {
        // 创建JPanel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建时间选择器
        JSlider timeSlider = buildTimeSlider();
        mainPanel.add(timeSlider);

        // 创建底部面板
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        bottomPanel.setLayout(new BorderLayout());

        // 创建时间显示标签
        JLabel timeShow = buildTimeLabel();
        timeShow.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        bottomPanel.add(timeShow, BorderLayout.WEST);

        // 创建操作按钮组
        JPanel actionBtnBox = buildActionButtonGroup();
        bottomPanel.add(actionBtnBox, BorderLayout.EAST);
        // 将底部面板添加到主面板
        mainPanel.add(bottomPanel);
        // 设置Scene和设置Stage
        mainFrame.add(mainPanel);

        this.bindSliderEvent(timeSlider, timeShow);
    }

    /**
     * 构建操作按钮组
     * <p>
     * 创建并返回一个包含多个操作按钮的面板，每次仅允许选择一个操作每个按钮对应一个操作，
     * 当被选中时，会执行相应的操作通过这个方法，我们可以集中管理所有的操作按钮，
     * 并确保它们之间互斥，即同时只能选择一个操作
     *
     * @return 包含操作按钮的面板
     */
    private JPanel buildActionButtonGroup() {
        // 创建ButtonGroup，确保按钮只能被选中一个
        ButtonGroup buttonGroup = new ButtonGroup();
        // 创建操作按钮组面板
        JPanel actionBtnBox = new JPanel();
        // 设置布局，使按钮垂直分布
        actionBtnBox.setLayout(new BoxLayout(actionBtnBox, BoxLayout.Y_AXIS));

        // 创建关机按钮，并设置为默认选中
        JRadioButton shutdownRadioButton = new JRadioButton("关机");
        shutdownRadioButton.setSelected(true);
        // 当关机按钮被选中时，执行关机操作
        shutdownRadioButton.addActionListener(e -> {
            if (shutdownRadioButton.isSelected()) {
                operate.action(Operate.SHUTDOWN);
            }
        });
        actionBtnBox.add(shutdownRadioButton);
        buttonGroup.add(shutdownRadioButton);

        // 创建重启按钮
        JRadioButton restartRadioButton = new JRadioButton("重启");
        // 当重启按钮被选中时，执行重启操作
        restartRadioButton.addActionListener(e -> {
            if (restartRadioButton.isSelected()) {
                operate.action(Operate.REBOOT);
            }
        });
        actionBtnBox.add(restartRadioButton);
        buttonGroup.add(restartRadioButton);

        // 创建响铃按钮
        JRadioButton ringingRadioButton = new JRadioButton("响铃");
        // 当响铃按钮被选中时，执行响铃操作
        ringingRadioButton.addActionListener(e -> {
            if (ringingRadioButton.isSelected()) {
                operate.action(Operate.RINGING);
            }
        });
        actionBtnBox.add(ringingRadioButton);
        buttonGroup.add(ringingRadioButton);
        return actionBtnBox;
    }

    /**
     * 创建并返回一个用于显示时间的JLabel对象
     * 该方法会尝试加载自定义的数字字体，如果失败，则使用默认的Serif字体
     *
     * @return JLabel对象，用于显示时间
     */
    private JLabel buildTimeLabel() {
        // 初始化时间显示标签，初始时间为00:00:00
        JLabel timeShow = new JLabel("00:00:00");

        // 尝试获取自定义数字字体资源的URL

        try {
            // 如果自定义字体资源URL不为null，则尝试创建并设置自定义字体
            timeShow.setFont(defaultNumberFont.deriveFont(75F));
        } catch (Exception e) {
            // 如果在加载或设置自定义字体时发生异常，则打印异常信息
            e.printStackTrace();
        }

        // 返回初始化完成的时间显示标签
        return timeShow;
    }

    private JLabel createLabel(String title, int size) {
        JLabel label = new JLabel(title);
        label.setFont(defaultTextFont.deriveFont((float) size));
        return label;
    }

    /**
     * 构建时间滑动条
     * <p>
     * 此方法用于创建并配置一个JSlider对象，该对象用于表示时间范围从0到150的滑动条
     * 滑动条上标记了特定时间点的标签，以便用户可以直观地选择时间范围
     *
     * @return JSlider 返回配置好的JSlider对象，用于表示时间滑动条
     */
    private JSlider buildTimeSlider() {
        // 创建一个JSlider对象，其值范围从0到150，初始值为0
        JSlider timeSlider = new JSlider(0, 150, 0);

        // 设置JSlider显示刻度标签
        timeSlider.setPaintLabels(true);
        // 设置JSlider的标签格式转换器
        // 用于将JSlider的值转换为字符串，并显示在标签上
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, createLabel("现在", 12));
        labelTable.put(30, createLabel("30分钟", 12));
        labelTable.put(60, createLabel("1小时", 12));
        labelTable.put(84, createLabel("3小时", 12));
        labelTable.put(102, createLabel("6小时", 12));
        labelTable.put(126, createLabel("12小时", 12));
        labelTable.put(150, createLabel("24小时", 12));

        // 将自定义标签设置到滑动条上
        timeSlider.setLabelTable(labelTable);

        // 设置主要刻度线的间隔
        timeSlider.setMajorTickSpacing(6);
        // 设置次要刻度线的间隔
        timeSlider.setMinorTickSpacing(1);
        // 绘制刻度线
        timeSlider.setPaintTicks(true);
        // 设置滑块移动时是否跳转到最近的刻度线
        timeSlider.setSnapToTicks(true);

        // 返回配置好的时间滑动条
        return timeSlider;
    }

    /**
     * 绑定滑动事件到时间滑块上，并更新时间显示标签
     * 当滑块值改变时，更新时间显示；当鼠标释放时，根据条件开始倒计时
     *
     * @param timeSlider 时间滑块组件，允许用户选择时间值
     * @param timeShow   时间显示标签，展示当前选择的时间
     */
    private void bindSliderEvent(JSlider timeSlider, JLabel timeShow) {
        // 使用AtomicReference来存储上一个滑块值，以确保线程安全
        AtomicReference<Integer> lastValue = new AtomicReference<>(0);

        // 添加数值变动监听器
        timeSlider.addChangeListener(e -> {
            int value = timeSlider.getValue();
            // 当滑块值变化超过0.5时，更新lastValue并刷新时间显示
            if (Math.abs(lastValue.get() - value) > 0.5) {
                lastValue.set(value);
                TimeInfo info = new TimeInfo(value);
                timeShow.setText(info.number());
                Logger.debug("timeSliderValue     >>>> " + value);
            }
        });

        // 添加鼠标事件监听器
        timeSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            // 鼠标释放时，根据条件开始倒计时
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                TimeInfo info = new TimeInfo(timeSlider.getValue());
                // 如果选择的时间大于0秒，开始倒计时
                if (info.second() > 0) {
                    // 从info.second开始倒计时，并且每秒更新timeShow的text内容为this.formatterSecond(info.second)
                    startCountdown(info, timeShow);
                }
            }

            // 鼠标按下时，取消计时器
            public void mousePressed(java.awt.event.MouseEvent evt) {
                // 如果timeline不为空，停止计时器
                if (timeline != null) {
                    timeline.stop();
                }
            }
        });
    }

    /**
     * 开始倒计时
     *
     * @param info     时间信息对象，包含倒计时的相关信息
     * @param timeShow 显示时间的 JLabel 控件
     */
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
                Logger.debug("operate >>>> " + operate.action());
                operate.execute();
            }
        });

        // 启动 Timeline
        timeline.start();
    }
}
