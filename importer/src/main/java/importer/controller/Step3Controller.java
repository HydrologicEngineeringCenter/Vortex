package importer.controller;

import com.google.inject.Inject;
import importer.WizardData;
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
import mil.army.usace.hec.vortex.io.BatchImporter;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;

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
        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 3");
        }

        List<String> inFiles = model.getInFiles()
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());

        List<String> variables = model.getSelectedVariables();

        Options geoOptions = Options.create();
        Optional.ofNullable(model.getClipDataSource()).ifPresent(entry -> {
            if (!entry.isEmpty())
                geoOptions.add("pathToShp", entry);
        });

        Optional.ofNullable(model.getTargetWkt()).ifPresent(entry -> {
            if (!entry.isEmpty())
                geoOptions.add("targetWkt", entry);
        });

        Optional.ofNullable(model.getTargetCellSize()).ifPresent(entry -> {
            if (!entry.isEmpty())
                geoOptions.add("targetCellSize", entry);
        });

        geoOptions.add("resamplingMethod", model.resamplingMethodProperty().getValue());

        String destination = model.getDestinationOut();

        Options writeOptions = Options.create();
        if (destination.toLowerCase().endsWith(".dss")) {
            writeOptions.add("partA", dssPathnamePartsController.getPartA());
            writeOptions.add("partB", dssPathnamePartsController.getPartB());
            writeOptions.add("partC", dssPathnamePartsController.getPartC());
            writeOptions.add("partF", dssPathnamePartsController.getPartF());
        }

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .geoOptions(geoOptions)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        importer.process();
    }

    private void setPersistedBrowseLocation(File file) {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "importer.properties" );

        try(OutputStream output = Files.newOutputStream(pathToProperties, CREATE)){
            Properties properties = new Properties();
            properties.setProperty("outFilePath", file.getPath());
            properties.store(output,null);
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private File getPersistedBrowseLocation() {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "importer.properties" );

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                String outFilePath = properties.getProperty("outFilePath");
                if (outFilePath == null){
                    return null;
                }
                if (Files.exists(Paths.get(outFilePath))) {
                    return new File(outFilePath);
                }
                return null;
            } catch (IOException e) {
                log.error(e.toString());
                return null;
            }
        }
        return null;
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


        dssPathnamePartsController.partC.textProperty().set("*");
        dssPathnamePartsController.partC.setDisable(true);
        dssPathnamePartsController.partD.textProperty().set("*");
        dssPathnamePartsController.partD.setDisable(true);
        dssPathnamePartsController.partE.textProperty().set("*");
        dssPathnamePartsController.partE.setDisable(true);

        Set<String> fParts = parts.get("fParts");
        if (fParts.size() == 1){
            dssPathnamePartsController.partF.textProperty().set(fParts.iterator().next());
        } else {
            dssPathnamePartsController.partF.textProperty().set("*");
        }
    }
}

