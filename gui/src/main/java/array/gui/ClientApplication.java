package array.gui;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) {
        new Client(this, stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
