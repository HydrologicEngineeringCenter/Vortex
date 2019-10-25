package normalizer.controller;

import com.google.inject.Inject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import mil.army.usace.hec.vortex.io.DataReader;
import normalizer.NormalizerWizard;
import normalizer.WizardData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

public class Step1Controller {

    private Logger log = LoggerFactory.getLogger(Step1Controller.class);

    @FXML
    TextField sourceFile;

    @FXML
    Button add;

    @FXML
    ListView<String> availableSourceGrids, selectedSourceGrids;

    @FXML
    Button addButton, removeButton;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        Image addImage = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        add.setGraphic(new ImageView(addImage));

        Image addVarImage = new Image(getClass().getResourceAsStream("/right-arrow-24.png"));
        addButton.setGraphic(new ImageView(addVarImage));

        Image removeVarImage = new Image(getClass().getResourceAsStream("/left-arrow-24.png"));
        removeButton.setGraphic(new ImageView(removeVarImage));

        sourceFile.textProperty().bindBidirectional(model.sourceFileProperty());

        availableSourceGrids.itemsProperty().bindBidirectional(model.availableSourceGridsProperty());
        availableSourceGrids.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableSourceGrids.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                ObservableList<String> selection = availableSourceGrids.getSelectionModel().getSelectedItems();
                TreeSet<String> selected = new TreeSet<>(this.selectedSourceGrids.getItems());
                selected.addAll(selection);
                this.selectedSourceGrids.setItems(FXCollections.observableArrayList(selected));
                model.removeAvailableSourceGrids(selection);
                availableSourceGrids.getSelectionModel().clearSelection();
            }
        });

        selectedSourceGrids.itemsProperty().bindBidirectional(model.selectedSourceGridsProperty());
        selectedSourceGrids.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedSourceGrids.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                ObservableList<String> selection = selectedSourceGrids.getSelectionModel().getSelectedItems();
                TreeSet<String> available = new TreeSet<>(this.availableSourceGrids.getItems());
                available.addAll(selection);
                this.availableSourceGrids.setItems(FXCollections.observableArrayList(available));
                model.removeSelectedSourceGrids(selection);
                selectedSourceGrids.getSelectionModel().clearSelection();
            }
        });
    }

    @FXML
    private void handleAddFile() {
        FileChooser fileChooser = new FileChooser();
        Optional<File> initialFilePath = Optional.ofNullable(getPersistedBrowseLocation());
        if (initialFilePath.isPresent()) {
            File filePath = initialFilePath.get();
            if (filePath.exists()) {
                fileChooser.setInitialDirectory(initialFilePath.get().getParentFile());
            }
        }

        // Set extension filters
        FileChooser.ExtensionFilter recognizedFilter = new FileChooser.ExtensionFilter(
                "All recognized files", "*.nc", "*.hdf", "*.grib", "*.gb2", "*.grb2", "*.grib2", "*.grb", "*.asc", "*.dss");
        fileChooser.getExtensionFilters().add(recognizedFilter);
        FileChooser.ExtensionFilter ncFilter = new FileChooser.ExtensionFilter(
                "netCDF datasets", "*.nc");
        fileChooser.getExtensionFilters().add(ncFilter);
        FileChooser.ExtensionFilter hdfFilter = new FileChooser.ExtensionFilter(
                "HDF datasets", "*.hdf");
        fileChooser.getExtensionFilters().add(hdfFilter);
        FileChooser.ExtensionFilter gribFilter = new FileChooser.ExtensionFilter(
                "GRIB datasets", "*.grib", "*.gb2", "*.grb2", "*.grib2", "*.grb");
        fileChooser.getExtensionFilters().add(gribFilter);

        FileChooser.ExtensionFilter ascFilter = new FileChooser.ExtensionFilter(
                "ASC datasets", "*.asc");
        fileChooser.getExtensionFilters().add(ascFilter);

        FileChooser.ExtensionFilter dssFilter = new FileChooser.ExtensionFilter(
                "DSS datasets", "*.dss");
        fileChooser.getExtensionFilters().add(dssFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(add.getScene().getWindow());

        if (file != null) {
            model.setSourceFile(file.toString());

            setPersistedBrowseLocation(file);

            Set<String> variables = DataReader.getVariables(file.toString());

            ObservableList<String> variableList = FXCollections.observableArrayList(variables);

            model.setAvailableSourceGrids(variableList);
            model.setSelectedSourceGrids(FXCollections.emptyObservableList());
        }
    }

    @FXML
    private void handleAdd() {
        ObservableList<String> selection = availableSourceGrids.getSelectionModel().getSelectedItems();
        TreeSet<String> selected = new TreeSet<>(this.selectedSourceGrids.getItems());
        selected.addAll(selection);
        this.selectedSourceGrids.setItems(FXCollections.observableArrayList(selected));
        model.removeAvailableSourceGrids(selection);
        availableSourceGrids.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRemove() {
        ObservableList<String> selection = selectedSourceGrids.getSelectionModel().getSelectedItems();
        TreeSet<String> available = new TreeSet<>(this.availableSourceGrids.getItems());
        available.addAll(selection);
        this.availableSourceGrids.setItems(FXCollections.observableArrayList(available));
        model.removeSelectedSourceGrids(selection);
        selectedSourceGrids.getSelectionModel().clearSelection();
    }

    @Validate
    public boolean validate() {

        if( model.getSourceFile().isEmpty() ) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Missing Field" );
            alert.setContentText( "Input dataset is required." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }
        if( selectedSourceGrids.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "No Variables Selected" );
            alert.setContentText( "At least one variable must be selected." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    @Submit
    public void submit() {


        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 1");
        }
    }

    public void setPersistedBrowseLocation(File file) {
        Preferences prefs = Preferences.userNodeForPackage(NormalizerWizard.class);
        if (Objects.nonNull(file)) {
            prefs.put("sourceFilePath", file.getPath());
        } else {
            prefs.remove("sourceFilePath");
        }
    }

    public File getPersistedBrowseLocation() {
        Preferences prefs = Preferences.userNodeForPackage(NormalizerWizard.class);
        String filePath = prefs.get("sourceFilePath", null);
        if (filePath != null) {
            return new File(filePath);
        } else {
            return null;
        }
    }
}
