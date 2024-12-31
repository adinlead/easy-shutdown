module cc.itez.tool.easyshutdown {
    requires jlayer;
    requires java.desktop;

    opens cc.itez.tool.easyshutdown.javafx to javafx.fxml;

    exports cc.itez.tool.easyshutdown.swing;
    exports cc.itez.tool.easyshutdown.javafx;
    exports cc.itez.tool.easyshutdown;
    opens cc.itez.tool.easyshutdown to javafx.fxml;
}
