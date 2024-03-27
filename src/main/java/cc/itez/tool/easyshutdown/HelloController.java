package cc.itez.tool.easyshutdown;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class HelloController {
    @FXML
    private Label timeShow;
    @FXML
    private Slider timeSlider;

    public HelloController() {
    }

    private String convertValueToNonLinear(double value) {
        return value + "H";
    }

}