package client;

import client.view.MainDashboard;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class Main extends Application {
    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("DOS/DDoS ATTACK SIMULATOR");

        Scene scene = new Scene(new MainDashboard(), 1360, 820);
        scene.getStylesheets().add(Main.class.getResource("/client/cyber.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1180);
        stage.setMinHeight(700);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
