package sanitizer;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WizardData {

    private static final Logger logger = Logger.getLogger(WizardData.class.getName());

    private final StringProperty inFile = new SimpleStringProperty();
    private final SimpleListProperty<String> availableVariables = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedVariables = new SimpleListProperty<>();
    private final StringProperty minimumThreshold = new SimpleStringProperty();
    private final StringProperty minimumReplacement = new SimpleStringProperty();
    private final StringProperty maximumThreshold = new SimpleStringProperty();
    private final StringProperty maximumReplacement = new SimpleStringProperty();
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
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, e::getMessage);
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
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, e::getMessage);
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

    public StringProperty lowerThresholdProperty() {
        return minimumThreshold;
    }

    public String getMinimumThreshold(){
        return minimumThreshold.get();
    }

    public StringProperty lowerReplacementProperty() {
        return minimumReplacement;
    }

    public String getMinimumReplacement(){
        return minimumReplacement.get();
    }

    public StringProperty upperThresholdProperty() {
        return maximumThreshold;
    }

    public StringProperty upperReplacementProperty() {
        return maximumReplacement;
    }

    public String getMaximumReplacement(){
        return maximumReplacement.get();
    }

    public String getMaximumThreshold(){
        return maximumThreshold.get();
    }

    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }

}

