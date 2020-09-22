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

    @FXML TextField startDate;
    @FXML TextField startTime;
    @FXML TextField endDate;
    @FXML TextField endTime;
    @FXML TextField interval;
    @FXML ComboBox<String> intervalType;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        startDate.textProperty().bindBidirectional(model.startDateProperty());
        endDate.textProperty().bindBidirectional(model.endDateProperty());
        startTime.textProperty().bindBidirectional(model.startTimeProperty());
        endTime.textProperty().bindBidirectional(model.endTimeProperty());
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
            LocalTime startTime = LocalTime.parse(model.getStartTime(), timeFormatter);
            LocalDate startD = LocalDate.parse(startDate.textProperty().get(), dateFormatter);
            ZonedDateTime startDateTime = ZonedDateTime.of(LocalDateTime.of(startD, startTime), ZoneId.of("UTC"));
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
            LocalTime endTime = LocalTime.parse(model.getStartTime(), timeFormatter);
            LocalDate endD = LocalDate.parse(endDate.textProperty().get(), dateFormatter);
            ZonedDateTime endDateTime = ZonedDateTime.of(LocalDateTime.of(endD, endTime), ZoneId.of("UTC"));
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
