module cc.itez.tool.easyshutdown {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens cc.itez.tool.easyshutdown to javafx.fxml;
    exports cc.itez.tool.easyshutdown;
}