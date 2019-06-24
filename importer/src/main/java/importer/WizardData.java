package importer;

import java.util.*;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class WizardData {

    private final SimpleListProperty<String> inFiles = new SimpleListProperty<>();
    private final SimpleListProperty<String> availableVariables = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedVariables = new SimpleListProperty<>();
    private final StringProperty destinationOut = new SimpleStringProperty();
    private final StringProperty clipDataSource = new SimpleStringProperty();
    private final StringProperty targetWkt = new SimpleStringProperty();
    private final StringProperty targetCellSize = new SimpleStringProperty();

    public SimpleListProperty<String> inFilesProperty() {
        return inFiles;
    }

    public void removeInFiles(ObservableList<String> files) {
        inFiles.removeAll(files);
    }

    public void setInFiles(ObservableList<String> files) {
        inFiles.set(files);
    }

    public void addInFiles(ObservableList<String> files) {
        Set<String> existing = new HashSet<>(inFiles);
        existing.addAll(new HashSet<>(files));
        List<String> list = new ArrayList<>(existing);
        Collections.sort(list);
        inFiles.set(FXCollections.observableArrayList(list));
    }

    public List<String> getInFiles(){
        return inFiles;
    }

    public void removeAvailableVariables(ObservableList<String> variables) {
        availableVariables.removeAll(variables);
    }

    public SimpleListProperty<String> availableVariablesProperty() {
        return availableVariables;
    }

    public void setAvailableVariables(ObservableList<String> variables) {
        availableVariables.set(variables);
    }

    public void addAvailableVariables(ObservableList<String> variables) {
        availableVariables.addAll(variables);
    }

    public void removeSelectedVariables(ObservableList<String> variables) {
        selectedVariables.removeAll(variables);
    }

    public List<String> getSelectedVariables() {
        return selectedVariables;
    }

    public SimpleListProperty<String> selectedVariablesProperty() {
        return selectedVariables;
    }

    public void setSelectedVariables(ObservableList<String> variables) {
        selectedVariables.set(variables);
    }

    public void addSelectedVariables(ObservableList<String> variables) {
        selectedVariables.addAll(variables);
    }

    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }

    public void setDestinationOut(String field5) {
        this.destinationOut.set(field5);
    }

    public String getClipDataSource() {
        return clipDataSource.get();
    }

    public StringProperty clipDataSourceProperty() {
        return clipDataSource;
    }

    public void setTargetWkt(String field) {
        this.targetWkt.set(field);
    }

    public String getTargetWkt() {
        return targetWkt.get();
    }

    public StringProperty targetWktProperty() {
        return targetWkt;
    }

    public void setTargetCellSize(String field) {
        this.targetCellSize.set(field);
    }

    public String getTargetCellSize() {
        return targetCellSize.get();
    }

    public StringProperty targetCellSizeProperty() {
        return targetCellSize;
    }
}

