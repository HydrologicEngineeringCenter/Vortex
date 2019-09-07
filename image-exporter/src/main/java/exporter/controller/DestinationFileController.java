package exporter.controller;

import exporter.ExporterWizard;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.prefs.Preferences;

public class DestinationFileController {

    @FXML
    TextField destination;

    @FXML
    Button browse;

    @FXML
    void initialize(){
        Image addImage = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        browse.setGraphic(new ImageView(addImage));
        destination.setText("");
    }

    String getDestination(){
        return destination.getText();
    }

    @FXML
    void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        Path initial = Paths.get(getDestination());
        if(initial.getParent().toFile().isDirectory()) {
            fileChooser.setInitialDirectory(initial.getParent().toFile());
        }
        fileChooser.setInitialFileName(initial.getFileName().toString());

        // Set extension filters
        FileChooser.ExtensionFilter dssFilter = new FileChooser.ExtensionFilter("Tiff files", "*.tiff");
        fileChooser.getExtensionFilters().add(dssFilter);

        // Show save file dialog
        File file = fileChooser.showSaveDialog(browse.getScene().getWindow());

        if (file != null) {
            destination.setText(file.getPath());
        }

        setPersistedBrowseLocation(file);
    }

    private void setPersistedBrowseLocation(File file) {
        Preferences prefs = Preferences.userNodeForPackage(ExporterWizard.class);
        if (Objects.nonNull(file)) {
            if (file.isFile()){
                prefs.put("outFilePath", file.getParent());
            } else {
                prefs.put("outFilePath", file.getPath());
            }
        } else {
            prefs.remove("outFilePath");
        }
    }

    File getPersistedBrowseLocation() {
        Preferences prefs = Preferences.userNodeForPackage(ExporterWizard.class);
        String filePath = prefs.get("outFilePath", null);
        if (Objects.nonNull(filePath)) {
            return new File(filePath);
        } else {
            return null;
        }
    }

}
