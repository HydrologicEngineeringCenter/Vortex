package calculator.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import calculator.WizardData;

import java.io.IOException;

public class Step2Controller {

    private final Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    private CheckBox lowerThresholdCheckBox;

    @FXML
    private Parent lowerThresholdFilterView;
    private LowerThresholdController lowerThresholdController;

    @FXML
    private VBox lowerThresholdFilterVBox;

    @FXML
    private CheckBox upperThresholdCheckBox;

    @FXML
    private Parent upperThresholdFilterView;
    private UpperThresholdController upperThresholdController;

    @FXML
    private VBox upperThresholdFilterVBox;

    @Inject
    private WizardData model;

    @FXML
    public void initialize() {
        lowerThresholdCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isLowerThresholdEnabled = newValue;
            if (isLowerThresholdEnabled) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/LowerThresholdView.fxml"));

                try {
                    lowerThresholdFilterView = fxmlLoader.load();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }

                lowerThresholdController = fxmlLoader.getController();
                lowerThresholdController.lowerThresholdProperty().bindBidirectional(model.lowerThresholdProperty());
                lowerThresholdController.lowerReplacementProperty().bindBidirectional(model.lowerReplacementProperty());

                lowerThresholdFilterVBox = ((VBox) lowerThresholdCheckBox.getParent());
                lowerThresholdFilterVBox.getChildren().add(lowerThresholdFilterView);

            } else {
                lowerThresholdFilterVBox.getChildren().remove(lowerThresholdFilterView);
                lowerThresholdController = null;
                model.lowerThresholdProperty().set("");
                model.lowerReplacementProperty().set("");
            }
        });

        upperThresholdCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isUpperThresholdEnabled = newValue;
            if (isUpperThresholdEnabled) {
                FXMLLoader fxmlLoader = new FXMLLoader( getClass().getResource("/fxml/UpperThresholdView.fxml"));

                try {
                    upperThresholdFilterView = fxmlLoader.load();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
                upperThresholdController = fxmlLoader.getController();
                upperThresholdController.upperThresholdProperty().bindBidirectional(model.upperThresholdProperty());
                upperThresholdController.upperReplacementProperty().bindBidirectional(model.upperReplacementProperty());

                upperThresholdFilterVBox = ((VBox) upperThresholdCheckBox.getParent());
                upperThresholdFilterVBox.getChildren().add(upperThresholdFilterView);

            } else {
                upperThresholdFilterVBox.getChildren().remove(upperThresholdFilterView);
                upperThresholdController = null;
                model.upperThresholdProperty().set("");
                model.upperReplacementProperty().set("");
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
