package clipper.controller;

import clipper.WizardData;
import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
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

public class GeoprocessingStepController implements BrowseLocationPersister {

    private Logger log = LoggerFactory.getLogger(GeoprocessingStepController.class);

    @FXML
    TextField clipDataSource;
    @FXML
    Button browse;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        clipDataSource.textProperty().bindBidirectional(model.clipDataSourceProperty());

        Image folderOpen = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        browse.setGraphic(new ImageView(folderOpen));
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
        FileChooser.ExtensionFilter shpFilter = new FileChooser.ExtensionFilter("Shapefiles", "*.shp");
        fileChooser.getExtensionFilters().add(shpFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(browse.getScene().getWindow());

        if (file != null) {
            clipDataSource.setText(file.getPath());
        }

        setPersistedBrowseLocation(file);
    }

    @Validate
    public boolean validate() {
        AtomicBoolean valid = new AtomicBoolean();
        valid.set(true);
        Optional.ofNullable(model.getClipDataSource()).ifPresent(entry -> {
            if(!entry.isEmpty()) {
                try {
                    Path pathToShp = Paths.get(entry);
                    if (!Files.exists(pathToShp)) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Invalid path");
                        alert.setContentText("Destination path does not exist.");
                        alert.initStyle(StageStyle.UTILITY);
                        alert.showAndWait();
                        valid.set(false);
                    }
                } catch (InvalidPathException e){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid path");
                    alert.setContentText(e.getReason());
                    alert.initStyle(StageStyle.UTILITY);
                    alert.showAndWait();
                    valid.set(false);
                }

            }
        });
        return valid.get();
    }

    @Submit
    public void submit() {
        if( log.isDebugEnabled() ) {
            log.debug("[SUBMIT] the user has completed geoprocessing writeOptions step");
        }
    }
}

