package cc.itez.tool.easyshutdown.swing;

import javax.swing.*;

public class Application {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(EasyShutdown::new);
    }
}
