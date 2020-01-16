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
import normalizer.WizardData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static java.nio.file.StandardOpenOption.CREATE;

public class Step2Controller {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    TextField normalsFile;

    @FXML
    ListView<String> availableNormalGrids, selectedNormalGrids;

    @FXML
    Button add;

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

        normalsFile.textProperty().bindBidirectional(model.normalsFileProperty());

        availableNormalGrids.itemsProperty().bindBidirectional(model.availableNormalGridsProperty());
        availableNormalGrids.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableNormalGrids.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                ObservableList<String> selection = availableNormalGrids.getSelectionModel().getSelectedItems();
                TreeSet<String> selected = new TreeSet<>(this.selectedNormalGrids.getItems());
                selected.addAll(selection);
                this.selectedNormalGrids.setItems(FXCollections.observableArrayList(selected));
                model.removeAvailableNormalGrids(selection);
                availableNormalGrids.getSelectionModel().clearSelection();
            }
        });

        selectedNormalGrids.itemsProperty().bindBidirectional(model.selectedNormalGridsProperty());
        selectedNormalGrids.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedNormalGrids.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                ObservableList<String> selection = selectedNormalGrids.getSelectionModel().getSelectedItems();
                TreeSet<String> available = new TreeSet<>(this.availableNormalGrids.getItems());
                available.addAll(selection);
                this.availableNormalGrids.setItems(FXCollections.observableArrayList(available));
                model.removeSelectedNormalGrids(selection);
                selectedNormalGrids.getSelectionModel().clearSelection();
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
            model.setNormalsFile(file.toString());

            setPersistedBrowseLocation(file);

            Set<String> variables = DataReader.getVariables(file.toString());

            ObservableList<String> variableList = FXCollections.observableArrayList(variables);

            model.setAvailableNormalGrids(variableList);
            model.setSelectedNormalGrids(FXCollections.emptyObservableList());
        }
    }

    @FXML
    private void handleAdd() {
        ObservableList<String> selection = availableNormalGrids.getSelectionModel().getSelectedItems();
        TreeSet<String> selected = new TreeSet<>(this.selectedNormalGrids.getItems());
        selected.addAll(selection);
        this.selectedNormalGrids.setItems(FXCollections.observableArrayList(selected));
        model.removeAvailableNormalGrids(selection);
        availableNormalGrids.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRemove() {
        ObservableList<String> selection = selectedNormalGrids.getSelectionModel().getSelectedItems();
        TreeSet<String> available = new TreeSet<>(this.availableNormalGrids.getItems());
        available.addAll(selection);
        this.availableNormalGrids.setItems(FXCollections.observableArrayList(available));
        model.removeSelectedNormalGrids(selection);
        selectedNormalGrids.getSelectionModel().clearSelection();
    }

    @Validate
    public boolean validate() {

        if( model.getNormalsFile().isEmpty() ) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Missing Field" );
            alert.setContentText( "Input dataset is required." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }
        if( selectedNormalGrids.getItems().isEmpty()) {
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
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "normalizer.properties" );

        try(OutputStream output = Files.newOutputStream(pathToProperties, CREATE)){
            Properties properties = new Properties();
            properties.setProperty("normalsFilePath", file.getPath());
            properties.store(output,null);
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private File getPersistedBrowseLocation() {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "normalizer.properties" );

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                String outFilePath = properties.getProperty("normalsFilePath");
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
}
