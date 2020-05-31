package sanitizer.controller;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class UpperThresholdController {
    @FXML
    TextField upperThreshold;

    @FXML
    TextField upperReplacement;

    @FXML
    public void initialize() {
        // No op
    }

    public StringProperty upperThresholdProperty() {
        return upperThreshold.textProperty();
    }

    public StringProperty upperReplacementProperty() {
        return upperReplacement.textProperty();
    }
}
