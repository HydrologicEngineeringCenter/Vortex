package sanitizer;

import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class WizardData {

    private final StringProperty inFile = new SimpleStringProperty();
    private final SimpleListProperty<String> availableVariables = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedVariables = new SimpleListProperty<>();
    private final StringProperty minimumThreshold = new SimpleStringProperty();
    private final StringProperty minimumReplacement = new SimpleStringProperty();
    private final BooleanProperty isReplaceWithNanAbove = new SimpleBooleanProperty();
    private final StringProperty maximumThreshold = new SimpleStringProperty();
    private final StringProperty maximumReplacement = new SimpleStringProperty();
    private final BooleanProperty isReplaceWithNanBelow = new SimpleBooleanProperty();
    private final StringProperty destinationOut = new SimpleStringProperty();

    public StringProperty inFileProperty() {
        return inFile;
    }

    public void setInFile(String file) {
        inFile.set(file);
    }

    public String getInFile(){
        return inFile.get();
    }

    public SimpleListProperty<String> availableVariablesProperty() {
        return availableVariables;
    }

    public void setAvailableVariables(ObservableList<String> grids) {
        if(getInFile().endsWith("dss")) {
            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("ddMMMuuuu:HHmm")
                        .toFormatter();
                grids.sort(Comparator.comparing(s -> LocalDateTime.parse(s.split("/")[4], formatter)));
            } catch (DateTimeParseException e) {
                e.printStackTrace();
            }
        }
        availableVariables.set(grids);
    }

    public SimpleListProperty<String> selectedVariablesProperty() {
        return selectedVariables;
    }

    public void setSelectedVariables(ObservableList<String> grids) {
        if(getInFile().endsWith("dss")) {
            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("ddMMMuuuu:HHmm")
                        .toFormatter();
                grids.sort(Comparator.comparing(s -> LocalDateTime.parse(s.split("/")[4], formatter)));
            } catch (DateTimeParseException e) {
                e.printStackTrace();
            }
        }
        selectedVariables.set(grids);
    }

    public List<String> getSelectedVariables(){
        return selectedVariables;
    }

    public void removeAvailableSourceGrids(ObservableList<String> variables) {
        availableVariables.removeAll(variables);
    }

    public void removeSelectedSourceGrids(ObservableList<String> variables) {
        selectedVariables.removeAll(variables);
    }

    public StringProperty minimumThresholdProperty() {
        return minimumThreshold;
    }

    public String getMinimumThreshold(){
        return minimumThreshold.get();
    }

    public StringProperty minimumReplacementProperty() {
        return minimumReplacement;
    }

    public String getMinimumReplacement(){
        return minimumReplacement.get();
    }

    public BooleanProperty isReplaceWithNanBelow () {
        return isReplaceWithNanBelow;
    }

    public StringProperty maximumThresholdProperty() {
        return maximumThreshold;
    }

    public StringProperty maximumReplacementProperty() {
        return maximumReplacement;
    }

    public String getMaximumReplacement(){
        return maximumReplacement.get();
    }

    public String getMaximumThreshold(){
        return maximumThreshold.get();
    }

    public BooleanProperty isReplaceWithNanAbove () {
        return isReplaceWithNanAbove;
    }

    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }

}

