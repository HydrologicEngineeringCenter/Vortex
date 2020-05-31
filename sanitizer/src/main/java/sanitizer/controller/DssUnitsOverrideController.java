package sanitizer.controller;

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
        unitsStringBox.setVisible(false);
        overrideDssUnitsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isOverrideDssUnits = newValue;
            if (isOverrideDssUnits) {
                unitsStringBox.setVisible(true);
                unitsString.setVisible(true);
                unitsString.setEditable(true);
            } else {
                unitsStringBox.setVisible(false);
                unitsString.setEditable(false);
                unitsString.setText("");
            }
        });
    }

    String getUnitsString(){
        return unitsString.getText();
    }

}
