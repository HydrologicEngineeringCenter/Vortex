package exporter.controller;

import exporter.ExporterWizard;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.prefs.Preferences;

public class DestinationDirController {

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
    private void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        Path initial = Paths.get(getDestination());
        if(initial.toFile().isDirectory()) {
            chooser.setInitialDirectory(initial.toFile());
        }

        // Show save file dialog
        File file = chooser.showDialog(browse.getScene().getWindow());

        if (file != null) {
            destination.setText(file.getPath());
        }

        setPersistedBrowseLocation(file);
    }

    private void setPersistedBrowseLocation(File file) {
        Preferences prefs = Preferences.userNodeForPackage(ExporterWizard.class);
        if (Objects.nonNull(file)) {
            prefs.put("outDirPath", file.getPath());
        } else {
            prefs.remove("outDirPath");
        }
    }

    File getPersistedBrowseLocation() {
        Preferences prefs = Preferences.userNodeForPackage(ExporterWizard.class);
        String filePath = prefs.get("outDirPath", null);
        if (Objects.nonNull(filePath)) {
            return new File(filePath);
        } else {
            return null;
        }
    }

}
