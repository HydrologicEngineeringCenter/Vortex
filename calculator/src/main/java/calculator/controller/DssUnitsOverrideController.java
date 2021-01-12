package calculator.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class DssUnitsOverrideController {

    @FXML CheckBox overrideDssUnitsCheckBox;
    @FXML HBox unitsStringBox;
    @FXML TextField unitsString;

    @FXML
    void initialize(){
        unitsString.setEditable(false);
        unitsString.setDisable(true);
        overrideDssUnitsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isOverride = newValue;
            if (isOverride) {
                unitsString.setEditable(true);
                unitsString.setDisable(false);
            } else {
                unitsString.setEditable(false);
                unitsString.setText("");
                unitsString.setDisable(true);
            }
        });
    }

    String getUnitsString(){
        return unitsString.getText();
    }

}
