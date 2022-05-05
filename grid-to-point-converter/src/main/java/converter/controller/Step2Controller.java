package converter.controller;

import com.google.inject.Inject;
import converter.WizardData;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import mil.army.usace.hec.vortex.geo.VectorUtils;
import mil.army.usace.hec.vortex.ui.BrowseLocationPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

public class Step2Controller implements BrowseLocationPersister {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML TextField zonesShapefile;
    @FXML Button browse;
    @FXML ComboBox<String> fieldComboBox;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {

        zonesShapefile.textProperty().bindBidirectional(model.zonesShapefileProperty());
        model.zonesShapefileProperty();

        zonesShapefile.textProperty().addListener((observable, oldValue, newValue) -> {
            Path pathToShp = Paths.get(model.getZonesShapefile());
            if (Files.exists(pathToShp) && !pathToShp.toString().isEmpty()) {
                Set<String> fields = VectorUtils.getFields(pathToShp);
                fieldComboBox.getItems().addAll(fields);
                fieldComboBox.getSelectionModel().select(0);

                for (String field : fields) {
                    if (field.equalsIgnoreCase("name")) {
                        fieldComboBox.getSelectionModel().select(field);
                        break;
                    }
                }
            } else {
                fieldComboBox.getItems().clear();
            }
        });

        model.fieldProperty().bind(fieldComboBox.getSelectionModel().selectedItemProperty());

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
            zonesShapefile.setText(file.getPath());
        }

        setPersistedBrowseLocation(file);
    }

    @Validate
    public boolean validate() {

//        if( model.inFilesProperty().isEmpty() ) {
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Error");
//            alert.setHeaderText( "Missing Field" );
//            alert.setContentText( "Input dataset is required." );
//            alert.initStyle(StageStyle.UTILITY);
//            alert.showAndWait();
//            return false;
//        }
        return true;
    }

    @Submit
    public void submit() {


        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 2");
        }
    }
}
