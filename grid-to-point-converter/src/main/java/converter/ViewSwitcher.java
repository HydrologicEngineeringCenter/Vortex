package converter;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.CREATE;

public class ViewSwitcher {
    private final Logger log = LoggerFactory.getLogger(ViewSwitcher.class);

    private static final String WINDOW_POSITION_X = "Window_Position_X";
    private static final String WINDOW_POSITION_Y = "Window_Position_Y";
    private static final String WINDOW_WIDTH = "Window_Width";
    private static final String WINDOW_HEIGHT = "Window_Height";
    private static final double DEFAULT_X = 50;
    private static final double DEFAULT_Y = 50;
    private static final double DEFAULT_WIDTH = 600;
    private static final double DEFAULT_HEIGHT = 400;

    public void switchView(Stage stage) {
        // Pull the saved preferences and set the stage size and start location
        stage.setX(DEFAULT_X);
        stage.setY(DEFAULT_Y);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);

        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "grid-to-point-converter.properties" );

        Path directory = pathToProperties.getParent();

        if (Files.notExists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                log.error(e.toString());
            }
        }

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);

                double x = Double.parseDouble(properties.getProperty(WINDOW_POSITION_X, String.valueOf(DEFAULT_X)));
                double y = Double.parseDouble(properties.getProperty(WINDOW_POSITION_Y, String.valueOf(DEFAULT_Y)));
                double width = Double.parseDouble(properties.getProperty(WINDOW_WIDTH, String.valueOf(DEFAULT_WIDTH)));
                double height = Double.parseDouble(properties.getProperty(WINDOW_HEIGHT, String.valueOf(DEFAULT_HEIGHT)));

                stage.setX(x);
                stage.setY(y);
                stage.setWidth(width);
                stage.setHeight(height);

            } catch (IOException e) {
                log.error(e.toString());
            }
        }

        stage.show();

        // When the stage closes store the current size and window location.

        stage.setOnCloseRequest((final WindowEvent event) -> {
            try (OutputStream output = Files.newOutputStream(pathToProperties, CREATE)) {
                Properties properties = new Properties();
                properties.setProperty(WINDOW_POSITION_X, String.valueOf(stage.getX()));
                properties.setProperty(WINDOW_POSITION_Y, String.valueOf(stage.getY()));
                properties.setProperty(WINDOW_WIDTH, String.valueOf(stage.getWidth()));
                properties.setProperty(WINDOW_HEIGHT, String.valueOf(stage.getHeight()));
                properties.store(output,null);
            } catch (IOException e) {
                log.error(e.toString());
            }
        });
    }
}
