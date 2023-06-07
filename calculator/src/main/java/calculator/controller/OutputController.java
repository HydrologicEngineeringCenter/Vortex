package calculator.controller;

import calculator.WizardData;
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
import mil.army.usace.hec.vortex.geo.ResamplingMethod;
import mil.army.usace.hec.vortex.math.BatchCalculator;
import mil.army.usace.hec.vortex.math.BatchGridCalculator;
import mil.army.usace.hec.vortex.math.Operation;
import mil.army.usace.hec.vortex.ui.BrowseLocationPersister;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OutputController implements BrowseLocationPersister {

    private final Logger logger = LoggerFactory.getLogger(OutputController.class);

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
        List<String> sourceGrids = model.getSelectedVariables();
        String destinationOut = model.getDestinationOut();

        Map<String, String> options = new HashMap<>();
        if (destinationOut.toLowerCase().endsWith(".dss")) {
            options.put("partA", dssPathnamePartsController.getPartA());
            options.put("partB", dssPathnamePartsController.getPartB());
            options.put("partC", dssPathnamePartsController.getPartC());
            options.put("partD", dssPathnamePartsController.getPartD());
            options.put("partE", dssPathnamePartsController.getPartE());
            options.put("partF", dssPathnamePartsController.getPartF());

            String unitsString = dssUnitsOverrideController.getUnitsString();
            if (!unitsString.isEmpty())
                options.put("units", unitsString);

            String dataType = dssDataTypeOverrideController.getSelectedItem();
            if (dataType != null && !dataType.isEmpty())
                options.put("dataType", dataType);
        }

        if (model.isConstantCompute().get()) {
            float multiplyValue;
            if (model.getMultiplyValue() != null && !model.getMultiplyValue().isEmpty()) {
                multiplyValue = Float.parseFloat(model.getMultiplyValue());
            } else {
                multiplyValue = Float.NaN;
            }

            float divideValue;
            if (model.getDivideValue() != null && !model.getDivideValue().isEmpty()) {
                divideValue = Float.parseFloat(model.getDivideValue());
            } else {
                divideValue = Float.NaN;
            }

            float addValue;
            if (model.getAddValue() != null && !model.getAddValue().isEmpty()) {
                addValue = Float.parseFloat(model.getAddValue());
            } else {
                addValue = Float.NaN;
            }

            float subtractValue;
            if (model.getSubtractValue() != null && !model.getSubtractValue().isEmpty()) {
                subtractValue = Float.parseFloat(model.getSubtractValue());
            } else {
                subtractValue = Float.NaN;
            }

            BatchCalculator batchCalculator = BatchCalculator.builder()
                    .pathToInput(pathToSource)
                    .variables(sourceGrids)
                    .multiplyValue(multiplyValue)
                    .divideValue(divideValue)
                    .addValue(addValue)
                    .subtractValue(subtractValue)
                    .destination(destinationOut)
                    .writeOptions(options)
                    .build();

            batchCalculator.process();
        } else {
            String pathToRaster = model.pathToRaster().get();

            ResamplingMethod resamplingMethod = ResamplingMethod.fromString(model.resamplingMethod().get());

            Operation operation = Operation.fromDisplayString(model.operation().get());

            BatchGridCalculator batchGridCalculator = BatchGridCalculator.builder()
                    .pathToInput(pathToSource)
                    .variables(sourceGrids)
                    .setOperation(operation)
                    .setPathToRaster(pathToRaster)
                    .setResamplingMethod(resamplingMethod)
                    .destination(destinationOut)
                    .writeOptions(options)
                    .build();

            batchGridCalculator.process();
        }

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

