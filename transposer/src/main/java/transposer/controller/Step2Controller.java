package transposer.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transposer.WizardData;

public class Step2Controller {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    private TextField angle;
    @FXML
    private TextField stormCenterX;
    @FXML
    private TextField stormCenterY;
    @FXML
    private TextField scaleFactor;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {

        angle.textProperty().bindBidirectional(model.angleProperty());
        stormCenterX.textProperty().bindBidirectional(model.stormCenterXProperty());
        stormCenterY.textProperty().bindBidirectional(model.stormCenterYProperty());
        scaleFactor.textProperty().bindBidirectional(model.scaleFactorProperty());

    }

    @Validate
    public boolean validate() {

        if (model.stormCenterXProperty().isEmpty().get() && model.stormCenterYProperty().isNotEmpty().get()
                || model.stormCenterXProperty().isNull().get() && model.stormCenterYProperty().isNotNull().get()
                || model.stormCenterXProperty().isNotEmpty().get() && model.stormCenterYProperty().isEmpty().get()
                || model.stormCenterXProperty().isNotNull().get() && model.stormCenterYProperty().isNull().get()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Both X and Y storm center coordinates are required.");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    @Submit
    public void submit() {


        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 1");
        }
    }

}
