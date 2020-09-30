package clipper;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import java.util.List;

public class WizardData {

    private final StringProperty inFile = new SimpleStringProperty();
    private final SimpleListProperty<String> availableVariables = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedVariables = new SimpleListProperty<>();
    private final StringProperty clipDataSource = new SimpleStringProperty();
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
        availableVariables.set(grids);
    }

    public SimpleListProperty<String> selectedVariablesProperty() {
        return selectedVariables;
    }

    public void setSelectedVariables(ObservableList<String> grids) {
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

    public String getClipDataSource() {
        return clipDataSource.get();
    }

    public StringProperty clipDataSourceProperty() {
        return clipDataSource;
    }

    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }

}

