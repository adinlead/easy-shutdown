package cc.itez.tool.easyshutdown;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Test extends Application {

    @Override
    public void start(Stage primaryStage) {
        Slider slider = new Slider(0, 150, 0);

        HBox labels = new HBox();
        labels.setPadding(new Insets(5, 10, 0, 10));
        labels.setSpacing(10);
        labels.getChildren().addAll(
                createLabel("1小时", (double) 60 / 150 * slider.getWidth()),
                createLabel("3小时", (double) 84 / 150 * slider.getWidth()),
                createLabel("6小时", (double) 102 / 150 * slider.getWidth()),
                createLabel("12小时", (double) 126 / 150 * slider.getWidth()),
                createLabel("24小时", (double) 150 / 150 * slider.getWidth())
        );

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(slider);
        borderPane.setBottom(labels);

        primaryStage.setScene(new Scene(borderPane, 400, 100));
        primaryStage.show();
    }

    private Label createLabel(String text, double layoutX) {
        Label label = new Label(text);
        label.setLayoutX(layoutX);
        return label;
    }

    public static void main(String[] args) {
        launch(args);
    }
}