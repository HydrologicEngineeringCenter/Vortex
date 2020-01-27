package importer.controller;

import com.google.inject.Inject;
import importer.WizardData;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import mil.army.usace.hec.vortex.geo.WktFactory;
import mil.army.usace.hec.vortex.ui.BrowseLocationPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GeoprocessingStepController implements BrowseLocationPersister {

    private Logger log = LoggerFactory.getLogger(GeoprocessingStepController.class);

    @FXML TextField clipDataSource;
    @FXML Button browse;
    @FXML Button browsePrj;
    @FXML Button selectProjection;
    @FXML Button selectCellSize;
    @FXML TextArea targetWkt;
    @FXML TextField targetCellSize;
    @FXML ComboBox<String> resamplingMethod;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        clipDataSource.textProperty().bindBidirectional(model.clipDataSourceProperty());
        targetWkt.textProperty().bindBidirectional(model.targetWktProperty());
        targetCellSize.textProperty().bindBidirectional(model.targetCellSizeProperty());
        model.resamplingMethodProperty().bind(resamplingMethod.getSelectionModel().selectedItemProperty());

        Image folderOpen = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        browse.setGraphic(new ImageView(folderOpen));
        browsePrj.setGraphic(new ImageView(folderOpen));
        Image globe = new Image(getClass().getResourceAsStream("/geography-16.png"));
        selectProjection.setGraphic(new ImageView(globe));
        Image grid = new Image(getClass().getResourceAsStream("/grid-16.png"));
        selectCellSize.setGraphic(new ImageView(grid));

        Set<String> resamplingMethods = new HashSet<>(new ArrayList<>(Arrays.asList("Nearest Neighbor", "Bilinear", "Average")));
        resamplingMethod.getItems().addAll(resamplingMethods);
        resamplingMethod.getSelectionModel().select(0);
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

    @FXML
    private void handleBrowsePrj() {
        FileChooser fileChooser = new FileChooser();
        Optional<File> initialFilePath = Optional.ofNullable(getPersistedBrowseLocation());
        if (initialFilePath.isPresent()) {
            File filePath = initialFilePath.get();
            if (filePath.exists()) {
                fileChooser.setInitialDirectory(initialFilePath.get().getParentFile());
            }
        }

        // Set extension filters
        FileChooser.ExtensionFilter shpFilter = new FileChooser.ExtensionFilter("Projection File", "*.prj");
        fileChooser.getExtensionFilters().add(shpFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(browse.getScene().getWindow());

        if (file != null) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                model.setTargetWkt(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSelectCommonProjection(){

        ArrayList<String> utmZones = new ArrayList<>();
        for (int i = 1; i <= 60; i++){
            utmZones.add("UTM" + i + "N");
            utmZones.add("UTM" + i + "S");
        }

        List<String> choices = new ArrayList<>();
        choices.add("SHG");
        choices.addAll(utmZones);

        ChoiceDialog<String> dialog = new ChoiceDialog<>("SHG", choices);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setTitle(null);
        dialog.setContentText("Select Projection:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(cs -> model.setTargetWkt(WktFactory.create(cs)));

    }

    @FXML
    private void handleSelectCellSize(){
        List<Integer> choices = new ArrayList<>(
                Arrays.asList(50, 100, 200, 500, 1000, 2000, 5000, 10000)
        );

        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(2000, choices);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setTitle(null);
        dialog.setContentText("Select Cell Size:");

        Optional<Integer> result = dialog.showAndWait();

        result.ifPresent(size -> model.setTargetCellSize(size.toString()));
    }

    @Validate
    public boolean validate() {
        AtomicBoolean valid = new AtomicBoolean();
        valid.set(true);
        Optional.ofNullable(model.getTargetCellSize()).ifPresent(entry -> {
            if(!entry.isEmpty()) {
                try {
                    double cellSize = Double.parseDouble(entry);
                    if (cellSize <= 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Invalid cell size");
                        alert.setContentText("Cell size must be greater than 0.");
                        alert.initStyle(StageStyle.UTILITY);
                        alert.showAndWait();
                        valid.set(false);
                    }
                } catch (NumberFormatException e){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid cell size");
                    alert.setContentText(e.getMessage());
                    alert.initStyle(StageStyle.UTILITY);
                    alert.showAndWait();
                    valid.set(false);
                }
            }
        });
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

