package exporter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WizardData {

    private final StringProperty inputFile = new SimpleStringProperty();
    private final StringProperty projectionWkt = new SimpleStringProperty();
    private final StringProperty cellSize = new SimpleStringProperty();
    private final StringProperty destinationOut = new SimpleStringProperty();

    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }

    public void setDestinationOut(String field5) {
        this.destinationOut.set(field5);
    }

    public String getInputFile() {
        return inputFile.get();
    }

    public StringProperty inputFileProperty() {
        return inputFile;
    }

    public void setProjectionWkt(String field) {
        this.projectionWkt.set(field);
    }

    public String getProjectionWkt() {
        return projectionWkt.get();
    }

    public StringProperty projectionWktProperty() {
        return projectionWkt;
    }

    public void setCellSize(String field) {
        this.cellSize.set(field);
    }

    public String getCellSize() {
        return cellSize.get();
    }

    public StringProperty cellSizeProperty() {
        return cellSize;
    }
}

