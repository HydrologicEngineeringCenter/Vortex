package converter.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.convert.GridToPointConverter;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import converter.ConverterWizard;
import converter.WizardData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.Preferences;

public class Step3Controller {

    private Logger log = LoggerFactory.getLogger(Step3Controller.class);

    @FXML
    VBox content;

    @FXML
    TextField destination;

    @FXML
    Button browse;

    @Inject
    WizardData model;

    private boolean dssInitialized = false;
    Parent dssPathnamePartsView;
    DssPathnamePartsController dssPathnamePartsController;

    @FXML
    public void initialize() {
        destination.textProperty().bindBidirectional(model.destinationOutProperty());

        destination.textProperty().addListener((obs, oldText, newText) -> {
            if (model.getDestinationOut().endsWith(".dss")) {

                if (!dssInitialized){
                    FXMLLoader fxmlLoader = new FXMLLoader( getClass().getResource("/fxml/DssPathnameParts.fxml"));
                    dssPathnamePartsView = null;
                    try {
                        dssPathnamePartsView = fxmlLoader.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    dssPathnamePartsController = fxmlLoader.getController();
                    initializeDssParts();
                    dssInitialized = true;
                } else {
                    content.getChildren().remove(destination.getScene().lookup("#dssPathnameParts"));
                }

                content.getChildren().add(dssPathnamePartsView);

            } else {
                content.getChildren().remove(destination.getScene().lookup("#dssPathnameParts"));
            }
        });

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
        FileChooser.ExtensionFilter dssFilter = new FileChooser.ExtensionFilter("DSS files", "*.dss");
        fileChooser.getExtensionFilters().add(dssFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(browse.getScene().getWindow());

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
        return true;
    }

    @Submit
    public void submit() {

        Path pathToSource = Paths.get(model.getInFile());
        Set<String> sourceGrids = new HashSet<>(model.getSelectedVariables());

        Path destination = Paths.get(model.getDestinationOut());

        Path zonesShapefile = Paths.get(model.getZonesShapefile());
        String field = model.getField();

        Options options = Options.create();
        if (destination.toString().toLowerCase().endsWith(".dss")) {
            options.add("partA", dssPathnamePartsController.getPartA());
            options.add("partB", dssPathnamePartsController.getPartB());
            options.add("partC", dssPathnamePartsController.getPartC());
            options.add("partD", dssPathnamePartsController.getPartD());
            options.add("partE", dssPathnamePartsController.getPartE());
            options.add("partF", dssPathnamePartsController.getPartF());
        }

        GridToPointConverter converter = GridToPointConverter.builder()
                .pathToGrids(pathToSource)
                .variables(sourceGrids)
                .pathToFeatures(zonesShapefile)
                .field(field)
                .destination(destination)
                .writeOptions(options)
                .build();

        converter.convert();

        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 3");
        }
    }

    private void setPersistedBrowseLocation(File file) {
        Preferences prefs = Preferences.userNodeForPackage(ConverterWizard.class);
        if (Objects.nonNull(file)) {
            prefs.put("outFilePath", file.getPath());
        } else {
            prefs.remove("outFilePath");
        }
    }

    private File getPersistedBrowseLocation() {
        Preferences prefs = Preferences.userNodeForPackage(ConverterWizard.class);
        String filePath = prefs.get("outFilePath", null);
        if (Objects.nonNull(filePath)) {
            return new File(filePath);
        } else {
            return null;
        }
    }

    private void initializeDssParts(){
        Map<String, Set<String>> parts = DssUtil.getPathnameParts(model.getSelectedVariables());

        Set<String> aParts = parts.get("aParts");
        if (aParts.size() == 1){
            dssPathnamePartsController.partA.textProperty().set(aParts.iterator().next());
        } else {
            dssPathnamePartsController.partA.textProperty().set("*");
        }

        dssPathnamePartsController.partB.textProperty().set("*");
        dssPathnamePartsController.partB.setDisable(true);

        Set<String> cParts = parts.get("cParts");
        if (cParts.size() == 1){
            dssPathnamePartsController.partC.textProperty().set(cParts.iterator().next());
        } else {
            dssPathnamePartsController.partC.textProperty().set("*");
        }

        dssPathnamePartsController.partD.textProperty().set("*");
        dssPathnamePartsController.partD.setDisable(true);
        dssPathnamePartsController.partE.textProperty().set("*");
        dssPathnamePartsController.partE.setDisable(true);

        Set<String> fParts = parts.get("fParts");
        if (fParts.size() == 1){
            dssPathnamePartsController.partF.textProperty().set(fParts.iterator().next());
        } else {
            dssPathnamePartsController.partF.textProperty().set("");
        }
    }
}

