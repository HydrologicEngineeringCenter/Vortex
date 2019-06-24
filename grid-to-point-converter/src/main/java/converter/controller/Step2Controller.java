package converter.controller;

import com.google.inject.Inject;
import converter.ConverterWizard;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import mil.army.usace.hec.vortex.geo.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import converter.WizardData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

public class Step2Controller {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML TextField zonesShapefile;
    @FXML Button browse;
    @FXML ComboBox<String> field;

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
                field.getItems().addAll(fields);
                field.getSelectionModel().select(0);
            } else {
                field.getItems().clear();
            }
        });

        model.fieldProperty().bind(field.getSelectionModel().selectedItemProperty());

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

    public void setPersistedBrowseLocation(File file) {
        Preferences prefs = Preferences.userNodeForPackage(ConverterWizard.class);
        if (file != null) {
            prefs.put("shpFilePath", file.getPath());
        } else {
            prefs.remove("shpFilePath");
        }
    }

    public File getPersistedBrowseLocation() {
        Preferences prefs = Preferences.userNodeForPackage(ConverterWizard.class);
        String filePath = prefs.get("shpFilePath", null);
        if (filePath != null) {
            return new File(filePath);
        } else {
            return null;
        }
    }

}
