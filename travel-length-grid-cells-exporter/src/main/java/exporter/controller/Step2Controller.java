package exporter.controller;

import com.google.inject.Inject;
import exporter.WizardData;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import mil.army.usace.hec.vortex.geo.TravelLengthGridCell;
import mil.army.usace.hec.vortex.geo.TravelLengthGridCellsReader;
import mil.army.usace.hec.vortex.geo.TravelLengthGridCellsWriter;
import mil.army.usace.hec.vortex.ui.BrowseLocationPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class Step2Controller implements BrowseLocationPersister {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    VBox content;

    @FXML
    TextField destination;

    @FXML
    Button browse;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        destination.textProperty().bindBidirectional(model.destinationOutProperty());

        Image browseImage = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        browse.setGraphic(new ImageView(browseImage));
    }

    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        Optional<File> initialFilePath = Optional.ofNullable(getPersistedBrowseLocation());
        if (initialFilePath.isPresent()) {
            File filePath = initialFilePath.get();
            if (filePath.exists()) {
                fileChooser.setInitialDirectory(initialFilePath.get().getParentFile());
            }
        }

        // Set extension filters
        FileChooser.ExtensionFilter dssFilter = new FileChooser.ExtensionFilter("Shapefiles", "*.shp");
        fileChooser.getExtensionFilters().add(dssFilter);

        // Show save file dialog
        File file = fileChooser.showSaveDialog(browse.getScene().getWindow());

        if (file != null) {
            destination.setText(file.getPath());
        }

        setPersistedBrowseLocation(file);
    }

    @Validate
    public boolean validate() {
        if( destination.getText() == null || destination.getText().isEmpty() ) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Missing Field" );
            alert.setContentText( "Destination file is required." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }

        TravelLengthGridCellsReader reader = TravelLengthGridCellsReader.builder()
                .pathToSource(model.getInputFile())
                .build();
        boolean isValid = reader.validate();

        if( !isValid ) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText( "Invalid Grid Cells" );
            alert.setContentText( "This file contains invalid grid cell definitions." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return true;
        }

        return true;
    }

    @Submit
    public void submit() {
        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 3");
        }

        List<TravelLengthGridCell> gridCells = TravelLengthGridCellsReader.builder()
                .pathToSource(model.getInputFile())
                .build()
                .read();

        TravelLengthGridCellsWriter writer = TravelLengthGridCellsWriter.builder()
                .pathToDestination(Paths.get(model.getDestinationOut()))
                .travelLengthGridCells(gridCells)
                .cellSize(Double.parseDouble(model.getCellSize()))
                .projection(model.getProjectionWkt())
                .build();

        writer.write();

    }
}

