package shifter.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shifter.WizardData;

public class Step2Controller {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML TextField interval;
    @FXML ComboBox<String> intervalType;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {

        interval.textProperty().bindBidirectional(model.intervalProperty());
        intervalType.getItems().addAll("Days", "Hours", "Minutes", "Seconds");
        intervalType.getSelectionModel().select("Hours");
        model.intervalTypeProperty().bind(intervalType.getSelectionModel().selectedItemProperty());

    }

    @Validate
    public boolean validate() {

//        if( model.inFilesProperty().isEmpty() ) {
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Error");
//            alert.setHeaderText( "Missing Field" );
//            alert.setContentText( "Input dataset is required." );
//            alert.initStyle(StageStyle.UTILITY);
//            alert.showAndWait();
//            return false;
//        }
        return true;
    }

    @Submit
    public void submit() {


        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 1");
        }
    }

}
