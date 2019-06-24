package normalizer;

import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

public class WizardData {

    private final StringProperty sourceFile = new SimpleStringProperty();
    private final SimpleListProperty<String> availableSourceGrids = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedSourceGrids = new SimpleListProperty<>();
    private final StringProperty normalsFile = new SimpleStringProperty();
    private final SimpleListProperty<String> availableNormalGrids = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedNormalGrids = new SimpleListProperty<>();
    private final SimpleObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<ZonedDateTime> startDateTime = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<ZonedDateTime> endDateTime = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>();
    private final StringProperty startTime = new SimpleStringProperty();
    private final StringProperty endTime = new SimpleStringProperty();
    private final StringProperty interval = new SimpleStringProperty();
    private final StringProperty intervalType = new SimpleStringProperty();
    private final StringProperty destinationOut = new SimpleStringProperty();

    public StringProperty sourceFileProperty() {
        return sourceFile;
    }

    public void setSourceFile(String file) {
        sourceFile.set(file);
    }

    public String getSourceFile(){
        return sourceFile.get();
    }

    public StringProperty normalsFileProperty() {
        return normalsFile;
    }

    public void setNormalsFile(String file) {
        normalsFile.set(file);
    }

    public String getNormalsFile(){
        return normalsFile.get();
    }

    public SimpleListProperty<String> availableSourceGridsProperty() {
        return availableSourceGrids;
    }

    public void setAvailableSourceGrids(ObservableList<String> grids) {
        availableSourceGrids.set(grids);
    }

    public List<String> getAvailableSourceGrids(){
        return availableSourceGrids;
    }

    public SimpleListProperty<String> selectedSourceGridsProperty() {
        return selectedSourceGrids;
    }

    public void setSelectedSourceGrids(ObservableList<String> grids) {
        selectedSourceGrids.set(grids);
    }

    public List<String> getSelectedSourceGrids(){
        return selectedSourceGrids;
    }

    public void removeAvailableSourceGrids(ObservableList<String> variables) {
        availableSourceGrids.removeAll(variables);
    }

    public void removeSelectedSourceGrids(ObservableList<String> variables) {
        selectedSourceGrids.removeAll(variables);
    }

    public SimpleListProperty<String> availableNormalGridsProperty() {
        return availableNormalGrids;
    }

    public void setAvailableNormalGrids(ObservableList<String> grids) {
        availableNormalGrids.set(grids);
    }

    public List<String> getSelectedNormalGrids(){
        return selectedNormalGrids;
    }

    public void removeAvailableNormalGrids(ObservableList<String> variables) {
        availableNormalGrids.removeAll(variables);
    }

    public void removeSelectedNormalGrids(ObservableList<String> variables) {
        selectedNormalGrids.removeAll(variables);
    }

    public SimpleListProperty<String> selectedNormalGridsProperty() {
        return selectedNormalGrids;
    }

    public void setSelectedNormalGrids(ObservableList<String> grids) {
        selectedNormalGrids.set(grids);
    }

    public List<String> getAvailableNormalGrids(){
        return availableNormalGrids;
    }

    public SimpleObjectProperty<LocalDate> startDateProperty() {
        return startDate;
    }

    public void setStartDate(LocalDate time) {
        startDate.set(time);
    }

    public LocalDate getStartDate(){
        return startDate.get();
    }

    public SimpleObjectProperty<ZonedDateTime> startDateTimeProperty() {
        return startDateTime;
    }

    public SimpleObjectProperty<ZonedDateTime> endDateTimeProperty() {
        return endDateTime;
    }

    public StringProperty startTimeProperty() {
        return startTime;
    }

    public void setStartTime(String time) {
        startTime.set(time);
    }

    public String getStartTime(){
        return startTime.get();
    }

    public SimpleObjectProperty<LocalDate> endDateProperty() {
        return endDate;
    }

    public void setEndDate(LocalDate date) {
        endDate.set(date);
    }

    public LocalDate getEndDate(){
        return endDate.get();
    }

    public StringProperty endTimeProperty() {
        return endTime;
    }

    public void setEndTime(String time) {
        endTime.set(time);
    }

    public String getEndTime(){
        return endTime.get();
    }

    public StringProperty intervalProperty() {
        return interval;
    }

    public void setInterval(String duration) {
        interval.set(duration);
    }

    public String getInterval(){
        return interval.get();
    }

    public StringProperty intervalTypeProperty() {
        return intervalType;
    }

    public void setIntervalType(String type) {
        intervalType.set(type);
    }

    public String getIntervalType(){
        return intervalType.get();
    }

    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }


}

