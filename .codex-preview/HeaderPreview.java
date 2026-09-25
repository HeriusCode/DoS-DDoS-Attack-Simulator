import client.view.MainDashboard;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class HeaderPreview extends Application {
    @Override public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        MainDashboard root = new MainDashboard();
        Scene scene = new Scene(root, 1600, 960);
        scene.getStylesheets().add(getClass().getResource("/client/cyber.css").toExternalForm());
        stage.setScene(scene); stage.show();
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(event -> {
            try {
                WritableImage image = root.snapshot(null, null); PixelReader pixels = image.getPixelReader();
                BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                for (int y=0; y<output.getHeight(); y++) for (int x=0; x<output.getWidth(); x++) output.setRGB(x,y,pixels.getArgb(x,y));
                ImageIO.write(output, "png", new File("D:/DoS_DDoS/.codex-preview/header.png"));
            } catch (Exception ex) { ex.printStackTrace(); } finally { Platform.exit(); }
        });
        pause.play();
    }
    public static void main(String[] args) { launch(args); }
}
