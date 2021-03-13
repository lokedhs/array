package array.gui;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApplication extends Application {
    private Client client = null;

    @Override
    public void start(Stage stage) {
        client = new Client(this, stage);
    }

    @Override
    public void stop() throws Exception {
        client.stopRequest();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
