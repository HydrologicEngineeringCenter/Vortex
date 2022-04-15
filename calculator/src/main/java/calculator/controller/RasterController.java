package calculator.controller;

import calculator.WizardData;
import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import mil.army.usace.hec.vortex.geo.ResamplingMethod;
import mil.army.usace.hec.vortex.math.Operation;
import mil.army.usace.hec.vortex.ui.BrowseLocationPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RasterController implements BrowseLocationPersister {

    private final Logger log = LoggerFactory.getLogger(RasterController.class);

    @FXML
    ComboBox<String> operationComboBox;
    @FXML
    TextField rasterDataSource;
    @FXML
    Button browse;
    @FXML
    ComboBox<String> resamplingMethod;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        Collection<String> operations = Arrays.stream(Operation.values())
                .map(Operation::getDisplayString)
                .collect(Collectors.toList());

        operationComboBox.getItems().addAll(operations);
        operationComboBox.getSelectionModel().select(Operation.MULTIPLY.getDisplayString());
        model.operation().bind(operationComboBox.getSelectionModel().selectedItemProperty());

        rasterDataSource.textProperty().bindBidirectional(model.pathToRaster());

        Image folderOpen = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        browse.setGraphic(new ImageView(folderOpen));

        Collection<String> resamplingMethods = Arrays.stream(ResamplingMethod.values())
                .map(ResamplingMethod::getDisplayString)
                .collect(Collectors.toList());

        resamplingMethod.getItems().addAll(resamplingMethods);
        resamplingMethod.getSelectionModel().select(ResamplingMethod.NEAREST_NEIGHBOR.getDisplayString());
        model.resamplingMethod().bind(resamplingMethod.getSelectionModel().selectedItemProperty());
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
        FileChooser.ExtensionFilter ascFilter = new FileChooser.ExtensionFilter("ASC", "*.asc");
        fileChooser.getExtensionFilters().add(ascFilter);

        FileChooser.ExtensionFilter tifFilter = new FileChooser.ExtensionFilter("TIFF", "*.tif", "*.tiff");
        fileChooser.getExtensionFilters().add(tifFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(browse.getScene().getWindow());

        if (file != null) {
            rasterDataSource.setText(file.getPath());
        }

        setPersistedBrowseLocation(file);
    }

    @Validate
    public boolean validate() {
        AtomicBoolean valid = new AtomicBoolean();
        valid.set(true);
        String pathToRaster = model.pathToRaster().get();

        if (pathToRaster == null || pathToRaster.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid path");
            alert.setContentText("Raster path is not provided.");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            valid.set(false);
            return false;
        }

        try {
            Path pathToShp = Paths.get(pathToRaster);
            if (!Files.exists(pathToShp)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid path");
                alert.setContentText("Raster path does not exist.");
                alert.initStyle(StageStyle.UTILITY);
                alert.showAndWait();
                valid.set(false);
            }
        } catch (InvalidPathException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid path");
            alert.setContentText(e.getReason());
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            valid.set(false);

        }

        return valid.get();
    }

    @Submit
    public void submit() {
        if( log.isDebugEnabled() ) {
            log.debug("[SUBMIT] the user has completed raster selection step");
        }
    }
}

