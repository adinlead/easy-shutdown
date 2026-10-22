package cc.itez.tool.easyshutdown.swing;

import cc.itez.tool.easyshutdown.Logger;
import cc.itez.tool.easyshutdown.TimeInfo;
import cc.itez.tool.easyshutdown.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 主窗口：只负责 UI 构建与事件接线。
 * 系统命令、倒计时、音频、配置全部委托给 core 层服务。
 */
public class EasyShutdown {

    /** 倒计时结束后的动作类型 */
    private enum Action {SHUTDOWN, REBOOT, RINGING}

    private final JFrame mainFrame = new JFrame();

    private final SystemOperator operator = SystemOperator.current();
    private final CountdownService countdown = new CountdownService();
    private final AudioService audio = new AudioService();
    private final AppConfig config = new AppConfig();

    private volatile Action action = Action.SHUTDOWN;

    private Locale locale;
    private Font defaultTextFont;
    private Font defaultNumberFont;
    private Point initialClick;

    public EasyShutdown() {
        this.init();
    }

    public void init() {
        this.initVariables();
        // 创建主UI窗口
        mainFrame.setTitle(locale.getTitle());
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
        defaultTextFont = loadFont(Resource.FONT_TEXT_DEFAULT);
        defaultNumberFont = loadFont(Resource.FONT_NUMBER_DEFAULT);
        String lang = config.lang();
        if (AppConfig.LANG_SYSTEM.equals(lang)) {
            lang = java.util.Locale.getDefault().getISO3Language();
        }
        locale = Locale.get(lang);
    }

    /**
     * 加载字体；字体属于非关键资源，失败时降级为系统默认字体而不是直接崩溃
     */
    private static Font loadFont(Resource resource) {
        try (InputStream stream = resource.stream()) {
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException e) {
            Logger.error(e, "字体【{}】加载失败，降级为系统默认字体", resource.path());
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
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
            Locale.Tray trayText = locale.getTrayMenus();
            SystemTray tray = SystemTray.getSystemTray();

            // 创建一个托盘图标
            Image image = Toolkit.getDefaultToolkit().getImage(Resource.IMG_LOGO_32.url())
                    .getScaledInstance(16, 16, Image.SCALE_SMOOTH); // 调整图像大小
            PopupMenu popup = new PopupMenu();

            // 创建一个退出选项
            MenuItem exitItem = new MenuItem();
            exitItem.setLabel(trayText.getExit());
            exitItem.setFont(defaultTextFont.deriveFont(16F));
            exitItem.addActionListener(e -> System.exit(0));

            popup.add(exitItem);

            // 创建托盘图标
            TrayIcon trayIcon = new TrayIcon(image, trayText.getTooltip(), popup);

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
            Logger.warn("系统不支持托盘功能");
        }
    }

    /**
     * 构建自定义状态栏
     * 该方法用于创建和配置一个自定义的状态栏，包括设置其外观、布局以及拖拽功能
     */
    public void buildCustomTitleBar() {
        Locale.TitleBar titleBarText = locale.getTitleBar();
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
            JLabel iconLabel = createLabel(titleBarText.getTitle(), 16);
            iconLabel.setHorizontalAlignment(SwingConstants.LEFT);
            iconLabel.setOpaque(false);
            iconLabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().createImage(Resource.IMG_LOGO_32.url())
                    .getScaledInstance(18, 18, Image.SCALE_SMOOTH)));

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.add(iconLabel);

            customStatusBar.add(titlePanel, BorderLayout.WEST);
        }
        { // 创建并配置右侧部分(按钮)
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            // 按钮右对齐
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            // 添加托盘按钮
            buttonPanel.add(createActionButton(Resource.IMG_BAR_BTN_TRAY, titleBarText.getTray(), e -> {
                Logger.info("即将进入托盘模式");
                mainFrame.setVisible(false);
            }, true));
            // 添加设置按钮
            buttonPanel.add(createActionButton(Resource.IMG_BAR_BTN_SETTING, titleBarText.getSetting(), e -> {
                Logger.info("即将进入设置");
                openSettings();
            }, true));
            // 添加关闭按钮
            buttonPanel.add(createActionButton(Resource.IMG_BAR_BTN_CLOSE, titleBarText.getClose(), e -> {
                Logger.info("即将退出程序");
                System.exit(0);
            }, true));

            // 将按钮加入到自定义状态栏
            customStatusBar.add(buttonPanel, BorderLayout.EAST);
        }
        mainFrame.add(customStatusBar, BorderLayout.NORTH);
    }

    /**
     * 设置弹窗：语言切换。配置写入 Preferences，重启后生效。
     */
    private void openSettings() {
        Map<String, String> locales = Locale.locales();
        if (locales.isEmpty()) {
            Logger.warn("未找到任何语言配置");
            return;
        }
        String[] names = locales.keySet().toArray(new String[0]);
        // 找到当前语言对应的显示名作为默认选中项
        String currentCode = config.lang();
        String currentName = null;
        for (Map.Entry<String, String> entry : locales.entrySet()) {
            if (entry.getValue().equals(currentCode)) {
                currentName = entry.getKey();
                break;
            }
        }

        String chosen = (String) JOptionPane.showInputDialog(
                mainFrame,
                "选择语言 / Language（重启后生效 / Restart to apply）",
                locale.getTitleBar().getSetting(),
                JOptionPane.PLAIN_MESSAGE,
                null,
                names,
                currentName != null ? currentName : names[0]);
        if (chosen != null) {
            config.lang(locales.get(chosen));
            Logger.info("语言已切换为 {}（{}），重启后生效", chosen, locales.get(chosen));
        }
    }

    /**
     * 创建一个带有图标和标题的动作按钮
     *
     * @param resource 图标的枚举，用于从类路径中加载图标
     * @param title    按钮的标题，用作工具提示和备用文本
     * @param listener 按钮的动作事件监听器
     * @return 返回一个配置好的JButton对象
     */
    private JButton createActionButton(Resource resource, String title, ActionListener listener, boolean enabled) {
        JButton button = new JButton();
        // 加载图片资源
        ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(resource.url())
                .getScaledInstance(22, 22, Image.SCALE_SMOOTH));
        icon.setDescription(title);
        button.setIcon(icon);
        button.setToolTipText(title);

        // 设置按钮的背景为透明
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        // 设置按钮的边框为1px黑色边框
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        if (enabled) {
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
        } else {
            button.setEnabled(false);
        }

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
     * 创建并返回一个包含多个操作按钮的面板，每次仅允许选择一个操作
     *
     * @return 包含操作按钮的面板
     */
    private JPanel buildActionButtonGroup() {
        Locale.ActionButtons actionButtonsText = locale.getActionButtons();
        // 创建ButtonGroup，确保按钮只能被选中一个
        ButtonGroup buttonGroup = new ButtonGroup();
        // 创建操作按钮组面板
        JPanel actionBtnBox = new JPanel();
        // 设置布局，使按钮垂直分布
        actionBtnBox.setLayout(new BoxLayout(actionBtnBox, BoxLayout.Y_AXIS));

        // 创建关机按钮，并设置为默认选中
        JRadioButton shutdownRadioButton = new JRadioButton(actionButtonsText.getShutdown());
        shutdownRadioButton.setSelected(true);
        shutdownRadioButton.addActionListener(e -> {
            if (shutdownRadioButton.isSelected()) {
                action = Action.SHUTDOWN;
            }
        });
        actionBtnBox.add(shutdownRadioButton);
        buttonGroup.add(shutdownRadioButton);

        // 创建重启按钮
        JRadioButton restartRadioButton = new JRadioButton(actionButtonsText.getRestart());
        restartRadioButton.addActionListener(e -> {
            if (restartRadioButton.isSelected()) {
                action = Action.REBOOT;
            }
        });
        actionBtnBox.add(restartRadioButton);
        buttonGroup.add(restartRadioButton);

        // 创建响铃按钮
        JRadioButton ringingRadioButton = new JRadioButton(actionButtonsText.getRinging());
        ringingRadioButton.addActionListener(e -> {
            if (ringingRadioButton.isSelected()) {
                action = Action.RINGING;
            }
        });
        actionBtnBox.add(ringingRadioButton);
        buttonGroup.add(ringingRadioButton);
        return actionBtnBox;
    }

    /**
     * 创建并返回一个用于显示时间的JLabel对象
     *
     * @return JLabel对象，用于显示时间
     */
    private JLabel buildTimeLabel() {
        JLabel timeShow = new JLabel("00:00:00");
        timeShow.setFont(defaultNumberFont.deriveFont(75F));
        return timeShow;
    }

    private JLabel createLabel(String title, int size) {
        JLabel label = new JLabel(title);
        label.setFont(defaultTextFont.deriveFont((float) size));
        return label;
    }

    /**
     * 构建时间滑动条
     *
     * @return JSlider 返回配置好的JSlider对象，用于表示时间滑动条
     */
    private JSlider buildTimeSlider() {
        Locale.TimeSlider timeSliderText = locale.getTimeSlider();
        // 创建一个JSlider对象，其值范围从0到150，初始值为0
        JSlider timeSlider = new JSlider(0, 150, 0);

        // 设置JSlider显示刻度标签
        timeSlider.setPaintLabels(true);
        // 用于将JSlider的值转换为字符串，并显示在标签上
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, createLabel(timeSliderText.getScale0(), 12));
        labelTable.put(30, createLabel(timeSliderText.getScale30(), 12));
        labelTable.put(60, createLabel(timeSliderText.getScale60(), 12));
        labelTable.put(84, createLabel(timeSliderText.getScale84(), 12));
        labelTable.put(102, createLabel(timeSliderText.getScale102(), 12));
        labelTable.put(126, createLabel(timeSliderText.getScale126(), 12));
        labelTable.put(150, createLabel(timeSliderText.getScale150(), 12));

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
        AtomicReference<Integer> lastValue = new AtomicReference<>(0);

        // 添加数值变动监听器
        timeSlider.addChangeListener(e -> {
            int value = timeSlider.getValue();
            if (value != lastValue.get()) {
                lastValue.set(value);
                timeShow.setText(TimeInfo.fromSliderValue(value).formatted());
                Logger.debug("timeSliderValue >>>> {}", value);
            }
        });

        // 添加鼠标事件监听器
        timeSlider.addMouseListener(new MouseAdapter() {
            // 鼠标释放时，根据条件开始倒计时
            public void mouseReleased(MouseEvent evt) {
                TimeInfo info = TimeInfo.fromSliderValue(timeSlider.getValue());
                if (info.seconds() > 0) {
                    countdown.start(info.seconds(),
                            // core 层回调在调度线程，切回 EDT 更新 UI
                            secs -> SwingUtilities.invokeLater(() -> timeShow.setText(TimeInfo.format(secs))),
                            () -> SwingUtilities.invokeLater(EasyShutdown.this::performAction));
                }
            }

            // 鼠标按下时，取消倒计时
            public void mousePressed(MouseEvent evt) {
                countdown.cancel();
            }
        });
    }

    /**
     * 倒计时结束，执行当前选中的动作。
     * 这里是 UI 边界：统一捕获 core 层异常，给用户可见反馈。
     */
    private void performAction() {
        Logger.debug("performAction >>>> {}", action);
        try {
            switch (action) {
                case SHUTDOWN -> operator.shutdown();
                case REBOOT -> operator.reboot();
                case RINGING -> {
                    audio.play(Resource.SOUND_DEFAULT.stream());
                    new RingingDialog(mainFrame, audio::stop).show();
                }
            }
        } catch (OperationException e) {
            Logger.error(e, "执行动作【{}】失败", action);
            JOptionPane.showMessageDialog(mainFrame, e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            Logger.error(e, "读取音频资源失败");
        }
    }
}
