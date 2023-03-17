package calculator;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import mil.army.usace.hec.vortex.ui.Util;

import java.util.List;
import java.util.logging.Logger;

public class WizardData {

    private static final Logger logger = Logger.getLogger(WizardData.class.getName());

    private final StringProperty inFile = new SimpleStringProperty();
    private final SimpleListProperty<String> availableVariables = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedVariables = new SimpleListProperty<>();

    private final BooleanProperty isConstantCompute = new SimpleBooleanProperty();

    private final StringProperty operation = new SimpleStringProperty();
    private final StringProperty pathToRaster = new SimpleStringProperty();
    private final StringProperty resamplingMethod = new SimpleStringProperty();

    private final StringProperty multiplyValue = new SimpleStringProperty();
    private final StringProperty divideValue = new SimpleStringProperty();
    private final StringProperty addValue = new SimpleStringProperty();
    private final StringProperty subtractValue = new SimpleStringProperty();

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
            Util.sortDssVariables(grids);
        }
        availableVariables.set(grids);
    }

    public SimpleListProperty<String> selectedVariablesProperty() {
        return selectedVariables;
    }

    public void setSelectedVariables(ObservableList<String> grids) {
        if(getInFile().endsWith("dss")) {
            Util.sortDssVariables(grids);
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

    public BooleanProperty isConstantCompute() {
        return isConstantCompute;
    }

    public StringProperty operation() {
        return operation;
    }

    public StringProperty pathToRaster() {
        return pathToRaster;
    }

    public StringProperty resamplingMethod() {
        return resamplingMethod;
    }

    public StringProperty multiplyValueProperty(){
        return multiplyValue;
    }

    public String getMultiplyValue(){
        return multiplyValue.get();
    }

    public StringProperty divideValueProperty() {
        return divideValue;
    }

    public String getDivideValue(){
        return divideValue.get();
    }

    public StringProperty addValueProperty() {
        return addValue;
    }

    public String getAddValue() {
        return addValue.get();
    }

    public StringProperty subtractValueProperty() {
        return subtractValue;
    }

    public String getSubtractValue() {
        return subtractValue.get();
    }

    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }

}

