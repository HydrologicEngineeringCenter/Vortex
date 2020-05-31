package sanitizer.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sanitizer.WizardData;

public class Step2Controller {

    private final Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    private TextField minimumThreshold;
    @FXML
    private TextField minimumReplacement;
    @FXML
    private CheckBox replaceWithNanBelow;
    @FXML
    private TextField maximumThreshold;
    @FXML
    private TextField maximumReplacement;
    @FXML
    private CheckBox replaceWithNanAbove;


    @Inject
    WizardData model;

    @FXML
    public void initialize() {

        minimumThreshold.textProperty().bindBidirectional(model.minimumThresholdProperty());
        minimumReplacement.textProperty().bindBidirectional(model.minimumReplacementProperty());
        replaceWithNanBelow.selectedProperty().bindBidirectional(model.isReplaceWithNanBelow());
        replaceWithNanBelow.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isReplaceWithNanBelow = newValue;
            if (isReplaceWithNanBelow) {
                minimumReplacement.setEditable(false);
                minimumReplacement.setText("");
            } else {
                minimumReplacement.setEditable(true);
            }
        });

        maximumThreshold.textProperty().bindBidirectional(model.maximumThresholdProperty());
        maximumReplacement.textProperty().bindBidirectional(model.maximumReplacementProperty());
        replaceWithNanAbove.selectedProperty().bindBidirectional(model.isReplaceWithNanAbove());
        replaceWithNanAbove.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isReplaceWithNanAbove = newValue;
            if (isReplaceWithNanAbove) {
                maximumReplacement.setEditable(false);
                maximumReplacement.setText("");
            } else {
                maximumReplacement.setEditable(true);
            }
        });

    }

    @Validate
    public boolean validate() {
        // No op
        return true;
    }

    @Submit
    public void submit() {

        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 1");
        }
    }

}
