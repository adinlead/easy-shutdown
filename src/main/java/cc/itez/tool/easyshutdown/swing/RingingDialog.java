package cc.itez.tool.easyshutdown.swing;

import javax.swing.*;
import java.awt.*;

/**
 * 响铃提醒弹窗：模态、置顶、无边框。
 * 点击"停止"按钮时通过 onStop 回调停止音频（音频生命周期由 AudioService 管理）。
 */
public final class RingingDialog {

    private final JFrame owner;
    private final Runnable onStop;

    public RingingDialog(JFrame owner, Runnable onStop) {
        this.owner = owner;
        this.onStop = onStop;
    }

    public void show() {
        JDialog dialog = new JDialog(owner, "倒计时已结束", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);

        JLabel label = new JLabel("倒计时已结束", SwingConstants.CENTER);
        label.setFont(new Font(Font.SERIF, Font.BOLD, 20));

        JButton closeButton = new JButton("停止");
        closeButton.setFont(new Font(Font.SERIF, Font.BOLD, 20));
        closeButton.addActionListener(e -> {
            onStop.run();
            dialog.dispose();
            owner.toFront();
        });

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
        layout.add(label, gbc);

        gbc.gridy = 1;
        layout.add(closeButton, gbc);

        dialog.add(layout);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
