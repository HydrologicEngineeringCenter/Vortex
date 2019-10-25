package shifter.controller;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shifter.ShifterWizard;
import shifter.WizardData;

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
    ListView<String> availableVariables, selectedVariables;

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

        sourceFile.textProperty().bindBidirectional(model.inFileProperty());

        availableVariables.itemsProperty().bindBidirectional(model.availableVariablesProperty());
        availableVariables.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableVariables.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                ObservableList<String> selection = availableVariables.getSelectionModel().getSelectedItems();
                TreeSet<String> selected = new TreeSet<>(this.selectedVariables.getItems());
                selected.addAll(selection);
                this.selectedVariables.setItems(FXCollections.observableArrayList(selected));
                model.removeAvailableSourceGrids(selection);
                availableVariables.getSelectionModel().clearSelection();
            }
        });

        selectedVariables.itemsProperty().bindBidirectional(model.selectedVariablesProperty());
        selectedVariables.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedVariables.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                ObservableList<String> selection = selectedVariables.getSelectionModel().getSelectedItems();
                TreeSet<String> available = new TreeSet<>(this.availableVariables.getItems());
                available.addAll(selection);
                this.availableVariables.setItems(FXCollections.observableArrayList(available));
                model.removeSelectedSourceGrids(selection);
                selectedVariables.getSelectionModel().clearSelection();
            }
        });
    }

    @FXML
    private void handleAddFile() {
        FileChooser fileChooser = new FileChooser();

        Optional.ofNullable(getPersistedBrowseLocation())
                .ifPresent(file -> {
                    File parent = file.getParentFile();
                    if (parent.exists()) {
                        fileChooser.setInitialDirectory(parent);
                    }
                });

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
            model.setInFile(file.toString());

            setPersistedBrowseLocation(file);

            Set<String> variables = DataReader.getVariables(file.toString());

            ObservableList<String> variableList = FXCollections.observableArrayList(variables);

            model.setAvailableVariables(variableList);
            model.setSelectedVariables(FXCollections.emptyObservableList());
        }
    }

    @FXML
    private void handleAdd() {
        ObservableList<String> selection = availableVariables.getSelectionModel().getSelectedItems();
        TreeSet<String> selected = new TreeSet<>(this.selectedVariables.getItems());
        selected.addAll(selection);
        this.selectedVariables.setItems(FXCollections.observableArrayList(selected));
        model.removeAvailableSourceGrids(selection);
        availableVariables.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRemove() {
        ObservableList<String> selection = selectedVariables.getSelectionModel().getSelectedItems();
        TreeSet<String> available = new TreeSet<>(this.availableVariables.getItems());
        available.addAll(selection);
        this.availableVariables.setItems(FXCollections.observableArrayList(available));
        model.removeSelectedSourceGrids(selection);
        selectedVariables.getSelectionModel().clearSelection();
    }

    @Validate
    public boolean validate() {

        if( model.getInFile() == null || model.getInFile().isEmpty() ) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Missing Field" );
            alert.setContentText( "Input dataset is required." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }
        if( selectedVariables.getItems().isEmpty()) {
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

    private void setPersistedBrowseLocation(File file) {
        Preferences prefs = Preferences.userNodeForPackage(ShifterWizard.class);
        if (Objects.nonNull(file)) {
            prefs.put("sourceFilePath", file.getPath());
        } else {
            prefs.remove("sourceFilePath");
        }
    }

    private File getPersistedBrowseLocation() {
        Preferences prefs = Preferences.userNodeForPackage(ShifterWizard.class);
        String filePath = prefs.get("sourceFilePath", null);
        if (filePath != null) {
            return new File(filePath);
        } else {
            return null;
        }
    }
}
