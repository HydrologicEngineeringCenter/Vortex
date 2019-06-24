package shifter;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.prefs.Preferences;

public class ViewSwitcher {
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
        Preferences pref = Preferences.userNodeForPackage(ShifterWizard.class);
        double x = pref.getDouble(WINDOW_POSITION_X, DEFAULT_X);
        double y = pref.getDouble(WINDOW_POSITION_Y, DEFAULT_Y);
        double width = pref.getDouble(WINDOW_WIDTH, DEFAULT_WIDTH);
        double height = pref.getDouble(WINDOW_HEIGHT, DEFAULT_HEIGHT);
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);

        stage.show();

        // When the stage closes store the current size and window location.

        stage.setOnCloseRequest((final WindowEvent event) -> {
            Preferences preferences = Preferences.userNodeForPackage(ShifterWizard.class);
            preferences.putDouble(WINDOW_POSITION_X, stage.getX());
            preferences.putDouble(WINDOW_POSITION_Y, stage.getY());
            preferences.putDouble(WINDOW_WIDTH, stage.getWidth());
            preferences.putDouble(WINDOW_HEIGHT, stage.getHeight());
        });
    }
}
