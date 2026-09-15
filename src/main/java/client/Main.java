package client;
import client.view.MainDashboard; import javafx.application.Application; import javafx.scene.Scene; import javafx.stage.Stage;
public final class Main extends Application { public void start(Stage stage){stage.setTitle("DOS/DDoS ATTACK SIMULATOR");Scene scene=new Scene(new MainDashboard(),1280,800);scene.getStylesheets().add(Main.class.getResource("/client/cyber.css").toExternalForm());stage.setScene(scene);stage.setMinWidth(1100);stage.setMinHeight(700);stage.show();} public static void main(String[] args){launch(args);} }
