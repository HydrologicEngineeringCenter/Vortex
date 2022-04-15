package calculator.controller;

import calculator.WizardData;
import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeTypeController {

    private final Logger log = LoggerFactory.getLogger(ComputeTypeController.class);

    @FXML
    private RadioButton constantButton;

    @FXML
    private RadioButton rasterButton;

    @Inject
    private WizardData model;

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        constantButton.setToggleGroup(group);
        constantButton.setSelected(true);

        rasterButton.setToggleGroup(group);

        constantButton.selectedProperty().bindBidirectional(model.isConstantCompute());
    }

    @Validate
    public boolean validate() {
        return true;
    }

    @Submit
    public void submit() {
        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 2");
        }
    }

}
