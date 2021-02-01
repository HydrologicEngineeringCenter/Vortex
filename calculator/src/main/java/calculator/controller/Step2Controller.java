package calculator.controller;

import calculator.WizardData;
import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Step2Controller {

    private final Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    private TextField multiplyTextField;

    @FXML
    private TextField divideTextField;

    @FXML
    private TextField addTextField;

    @FXML
    private TextField subtractTextField;

    @Inject
    private WizardData model;

    @FXML
    public void initialize() {
        multiplyTextField.textProperty().bindBidirectional(model.multiplyValueProperty());
        divideTextField.textProperty().bindBidirectional(model.divideValueProperty());
        addTextField.textProperty().bindBidirectional(model.addValueProperty());
        subtractTextField.textProperty().bindBidirectional(model.subtractValueProperty());


    }

    @Validate
    public boolean validate() {
        int count = 0;
        String multiplyText = multiplyTextField.getText();
        String divideText = divideTextField.getText();
        String addText = addTextField.getText();
        String subtractText = subtractTextField.getText();

        if (multiplyText != null && !multiplyText.isEmpty())
            count++;
        if (divideText != null && !divideText.isEmpty())
            count++;
        if (addText != null && !addText.isEmpty())
            count++;
        if(subtractText != null && !subtractText.isEmpty())
            count++;
        if (count != 1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Exceed number of constant value entries");
            alert.setContentText("Only one constant value entry allowed");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    @Submit
    public void submit() {
        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 2");
        }
    }

}
