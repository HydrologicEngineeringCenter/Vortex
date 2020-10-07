package sanitizer.controller;

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
import mil.army.usace.hec.vortex.math.BatchSanitizer;
import mil.army.usace.hec.vortex.ui.BrowseLocationPersister;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sanitizer.WizardData;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Step3Controller implements BrowseLocationPersister {

    private final Logger logger = LoggerFactory.getLogger(Step3Controller.class);

    @FXML
    private VBox content;

    @FXML
    private TextField destination;

    @FXML
    private Button browse;

    @Inject
    private WizardData model;

    private boolean dssInitialized = false;
    private Parent dssPathnamePartsView;
    private DssPathnamePartsController dssPathnamePartsController;

    private Parent dssUnitsOverrideView;
    private DssUnitsOverrideController dssUnitsOverrideController;

    private Parent dssDataTypeOverrideView;
    private DssDataTypeOverrideController dssDataTypeOverrideController;

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

                    FXMLLoader unitsLoader = new FXMLLoader( getClass().getResource("/fxml/DssUnitsOverride.fxml"));
                    dssUnitsOverrideView = null;
                    try {
                        dssUnitsOverrideView = unitsLoader.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dssUnitsOverrideController = unitsLoader.getController();

                    FXMLLoader dataTypeLoader = new FXMLLoader( getClass().getResource("/fxml/DssDataTypeOverride.fxml"));
                    dssDataTypeOverrideView = null;
                    try {
                        dssDataTypeOverrideView = dataTypeLoader.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dssDataTypeOverrideController = dataTypeLoader.getController();

                } else {
                    content.getChildren().remove(destination.getScene().lookup("#dssPathnameParts"));
                    content.getChildren().remove(destination.getScene().lookup("#dssUnitsOverride"));
                    content.getChildren().remove(destination.getScene().lookup("#dssDataTypeOverride"));
                }

                content.getChildren().add(dssPathnamePartsView);
                content.getChildren().add(dssUnitsOverrideView);
                content.getChildren().add(dssDataTypeOverrideView);


            } else {
                content.getChildren().remove(destination.getScene().lookup("#dssPathnameParts"));
                content.getChildren().remove(destination.getScene().lookup("#dssUnitsOverride"));
                content.getChildren().remove(destination.getScene().lookup("#dssDataTypeOverride"));
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

        String pathToSource = model.getInFile();
        List<String> sourceGrids = model.getSelectedVariables();
        String destinationOut = model.getDestinationOut();

        double minimumThreshold;
        if (model.getMinimumThreshold() != null && !model.getMinimumThreshold().isEmpty()) {
            minimumThreshold = Double.parseDouble(model.getMinimumThreshold());
        } else {
            minimumThreshold = -Double.MAX_VALUE;
        }

        float minimumReplacement;
        if (model.getMinimumReplacement() != null && !model.getMinimumReplacement().isEmpty()) {
            minimumReplacement = Float.parseFloat(model.getMinimumReplacement());
        } else {
            minimumReplacement = Float.NaN;
        }

        double maximumThreshold;
        if (model.getMaximumThreshold() != null && !model.getMaximumThreshold().isEmpty()) {
            maximumThreshold = Double.parseDouble(model.getMaximumThreshold());
        } else {
            maximumThreshold = Double.MAX_VALUE;
        }

        float maximumReplacement;
        if (model.getMaximumReplacement() != null && !model.getMaximumReplacement().isEmpty()) {
            maximumReplacement = Float.parseFloat(model.getMaximumReplacement());
        } else {
            maximumReplacement = Float.NaN;
        }

        Options options = Options.create();
        if (destinationOut.toLowerCase().endsWith(".dss")) {
            options.add("partA", dssPathnamePartsController.getPartA());
            options.add("partB", dssPathnamePartsController.getPartB());
            options.add("partC", dssPathnamePartsController.getPartC());
            options.add("partD", dssPathnamePartsController.getPartD());
            options.add("partE", dssPathnamePartsController.getPartE());
            options.add("partF", dssPathnamePartsController.getPartF());

            String unitsString = dssUnitsOverrideController.getUnitsString();
            if (!unitsString.isEmpty())
                options.add("units", unitsString);

            String dataType = dssDataTypeOverrideController.getSelectedItem();
            if (dataType != null && !dataType.isEmpty())
                options.add("dataType", dataType);
        }

        BatchSanitizer batchSanitizer = BatchSanitizer.builder()
                .pathToInput(pathToSource)
                .variables(sourceGrids)
                .minimumThreshold(minimumThreshold)
                .minimumReplacementValue(minimumReplacement)
                .maximumThreshold(maximumThreshold)
                .maximumReplacementValue(maximumReplacement)
                .destination(destinationOut)
                .writeOptions(options)
                .build();

        batchSanitizer.process();

        if (logger.isDebugEnabled()) {
            logger.debug("[SUBMIT] the user has completed step 3");
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

