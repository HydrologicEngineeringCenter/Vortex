package shifter.controller;

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
import mil.army.usace.hec.vortex.math.Shifter;
import mil.army.usace.hec.vortex.ui.BrowseLocationPersister;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shifter.WizardData;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class Step3Controller implements BrowseLocationPersister {

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

        Optional.ofNullable(getPersistedBrowseLocation()).ifPresent(file -> fileChooser.setInitialDirectory(file.getParentFile()));

        // Set extension filters
        FileChooser.ExtensionFilter dssFilter = new FileChooser.ExtensionFilter("DSS files", "*.dss");
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
        return true;
    }

    @Submit
    public void submit() {

        String pathToSource = model.getInFile();
        Set<String> sourceGrids = new HashSet<>(model.getSelectedVariables());

        String intervalType = model.getIntervalType();
        Duration interval;
        switch (intervalType) {
            case "Days":
                interval = Duration.ofDays(Integer.parseInt(model.getInterval()));
                break;
            case "Hours":
                interval = Duration.ofHours(Integer.parseInt(model.getInterval()));
                break;
            case "Seconds":
                interval = Duration.ofSeconds(Integer.parseInt(model.getInterval()));
                break;
            default:
                interval = Duration.ofMinutes(Integer.parseInt(model.getInterval()));
                break;
        }

        String destination = model.getDestinationOut();

        Map<String, String> options = new HashMap<>();
        if (destination.toLowerCase().endsWith(".dss")) {
            options.put("partA", dssPathnamePartsController.getPartA());
            options.put("partB", dssPathnamePartsController.getPartB());
            options.put("partC", dssPathnamePartsController.getPartC());
            options.put("partD", dssPathnamePartsController.getPartD());
            options.put("partE", dssPathnamePartsController.getPartE());
            options.put("partF", dssPathnamePartsController.getPartF());
        }

        Shifter shifter = Shifter.builder()
                .shift(interval)
                .pathToFile(pathToSource)
                .grids(sourceGrids)
                .destination(destination)
                .writeOptions(options)
                .build();

        shifter.shift();

        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 3");
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

        Set<String> bParts = parts.get("bParts");
        if (bParts.size() == 1){
            dssPathnamePartsController.partB.textProperty().set(bParts.iterator().next());
        } else {
            dssPathnamePartsController.partB.textProperty().set("*");
        }

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

