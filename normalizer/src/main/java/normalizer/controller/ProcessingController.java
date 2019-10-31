package normalizer.controller;

import com.google.inject.Inject;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import normalizer.WizardData;

public class ProcessingController {

    @FXML private TextArea console;

    @Inject private WizardData model;

    @FXML
    public void initialize() {
        console.textProperty().bindBidirectional(model.consoleTextProperty());
        console.setEditable(false);
        console.textProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
            console.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
            //use Double.MIN_VALUE to scroll to the top
        });
    }
}
