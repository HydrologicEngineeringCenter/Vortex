package importer.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;

public class DssDataTypeOverrideController {

    @FXML
    CheckBox overrideDssDataTypeCheckBox;
    @FXML
    HBox dataTypeBox;
    @FXML
    ComboBox<String> dataType;

    @FXML
    void initialize(){

        dataType.setEditable(false);
        dataType.setDisable(true);
        overrideDssDataTypeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isOverride = newValue;
            if (isOverride) {
                dataType.getItems().addAll(FXCollections.observableArrayList(
                        "INST-VAL",
                        "PER-CUM",
                        "PER-AVER",
                        "INST-CUM"
                ));
                dataType.setDisable(false);
            } else {
                dataType.getSelectionModel().clearSelection();
                dataType.getItems().clear();
                dataType.setDisable(true);
            }
        });
    }

    String getSelectedItem(){
        return dataType.getSelectionModel().getSelectedItem();
    }

}
