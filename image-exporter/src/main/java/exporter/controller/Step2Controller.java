package exporter.controller;

import com.google.inject.Inject;
import exporter.ExporterWizard;
import exporter.WizardData;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.io.ImageFileType;
import mil.army.usace.hec.vortex.util.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.Preferences;

public class Step2Controller {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML TextField destinationDir;
    @FXML Button browse;
    @FXML TextField baseName;
    @FXML ComboBox<ImageFileType> format;

    @Inject
    WizardData model;

    @FXML
    void initialize(){
        Image addImage = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        browse.setGraphic(new ImageView(addImage));

        File persistedDir = getPersistedBrowseLocation();
        String initialDir;
        if (persistedDir != null && persistedDir.exists()){
            initialDir = persistedDir.getPath();
        } else {
            initialDir = System.getProperty("user.home");
        }
        destinationDir.setText(initialDir);

        model.inFileProperty().addListener((observable, oldValue, newValue) -> {
            if (new File(model.getInFile()).exists()) {
                String fileNameIn = Paths.get(model.getInFile()).getFileName().toString();
                String fileNameInSansExt = fileNameIn.substring(0, fileNameIn.lastIndexOf('.'));
                baseName.setText(fileNameInSansExt);
            }
        });

        format.getItems().setAll(Arrays.asList(ImageFileType.values()));
        format.getSelectionModel().select(0);
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        Path initial = Paths.get(destinationDir.getText());
        if(initial.toFile().isDirectory()) {
            chooser.setInitialDirectory(initial.toFile());
        }

        // Show save file dialog
        File file = chooser.showDialog(browse.getScene().getWindow());

        if (file != null) {
            destinationDir.setText(file.getPath());
        }

        setPersistedBrowseLocation(file);
    }

    @Validate
    public boolean validate() {
        return true;
    }

    @Submit
    public void submit() {

        String pathToSource = model.getInFile();
        Set<String> variables = new HashSet<>(model.getSelectedVariables());

        variables.forEach(variable -> {
            List<VortexData> grids = DataReader.builder()
                    .path(pathToSource)
                    .variable(variable)
                    .build()
                    .getDtos();

            grids.forEach(grid -> {
                String fileName = ImageUtils.generateFileName(baseName.getText(), (VortexGrid) grid, format.getValue());
                Path destination = Paths.get(destinationDir.getText(), fileName);
                DataWriter writer = DataWriter.builder()
                        .data(grids)
                        .destination(destination)
                        .build();

                writer.write();
            });
        });

        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 2");
        }
    }

    private void setPersistedBrowseLocation(File file) {
        Preferences prefs = Preferences.userNodeForPackage(ExporterWizard.class);
        if (Objects.nonNull(file)) {
            prefs.put("outDirPath", file.getPath());
        } else {
            prefs.remove("outDirPath");
        }
    }

    private File getPersistedBrowseLocation() {
        Preferences prefs = Preferences.userNodeForPackage(ExporterWizard.class);
        String filePath = prefs.get("outDirPath", null);
        if (Objects.nonNull(filePath)) {
            return new File(filePath);
        } else {
            return null;
        }
    }
}

