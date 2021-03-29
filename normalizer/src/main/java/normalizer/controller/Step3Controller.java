package normalizer.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.StageStyle;
import normalizer.WizardData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Step3Controller {

    private Logger log = LoggerFactory.getLogger(Step3Controller.class);

    @FXML TextField startDateTextField;
    @FXML TextField startTimeTextField;
    @FXML TextField endDateTextField;
    @FXML TextField endTimeTextField;
    @FXML TextField interval;
    @FXML ComboBox<String> intervalType;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        startDateTextField.textProperty().bindBidirectional(model.startDateProperty());
        endDateTextField.textProperty().bindBidirectional(model.endDateProperty());
        startTimeTextField.textProperty().bindBidirectional(model.startTimeProperty());
        endTimeTextField.textProperty().bindBidirectional(model.endTimeProperty());
        interval.textProperty().bindBidirectional(model.intervalProperty());
        intervalType.getItems().addAll("Days", "Hours", "Minutes", "Seconds");
        intervalType.getSelectionModel().select("Days");
        model.intervalTypeProperty().bind(intervalType.getSelectionModel().selectedItemProperty());

    }

    @Validate
    public boolean validate() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        try {
            String startTimeString = model.getStartTime();
            LocalTime startTime = LocalTime.parse(model.getStartTime(), timeFormatter);
            LocalDate startDate;
            if (startTimeString.equals("2400")) {
                startDate = LocalDate.parse(startDateTextField.textProperty().get(), dateFormatter).plusDays(1);
            } else {
                startDate = LocalDate.parse(startDateTextField.textProperty().get(), dateFormatter);
            }
            ZonedDateTime startDateTime = ZonedDateTime.of(LocalDateTime.of(startDate, startTime), ZoneId.of("UTC"));
            model.startDateTimeProperty().set(startDateTime);
        } catch (DateTimeParseException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Parse Exception" );
            alert.setContentText( "Could not parse start date/time." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }

        try {
            String endTimeString = model.getEndTime();
            LocalTime endTime = LocalTime.parse(model.getEndTime(), timeFormatter);
            LocalDate endDate;
            if (endTimeString.equals("2400")) {
                endDate = LocalDate.parse(endDateTextField.textProperty().get(), dateFormatter).plusDays(1);
            } else {
                endDate = LocalDate.parse(endDateTextField.textProperty().get(), dateFormatter);
            }
            ZonedDateTime endDateTime = ZonedDateTime.of(LocalDateTime.of(endDate, endTime), ZoneId.of("UTC"));
            model.endDateTimeProperty().set(endDateTime);
        } catch (DateTimeParseException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Parse Exception" );
            alert.setContentText( "Could not parse end date/time." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }

        try {
            Integer.parseInt(model.getInterval());
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Parse Exception" );
            alert.setContentText( "Could not parse end interval." );
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
