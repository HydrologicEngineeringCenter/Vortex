package sanitizer.controller;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LowerThresholdController {
    @FXML
    private TextField lowerThreshold;

    @FXML
    private TextField lowerReplacement;

    @FXML
    public void initialize() {
        // No op
    }

    public StringProperty lowerThresholdProperty() {
        return lowerThreshold.textProperty();
    }

    public StringProperty lowerReplacementProperty() {
        return lowerReplacement.textProperty();
    }
}
