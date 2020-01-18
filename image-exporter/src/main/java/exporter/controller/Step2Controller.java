package exporter.controller;

import com.google.inject.Inject;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardOpenOption.CREATE;

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
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "image-exporter.properties" );

        try(OutputStream output = Files.newOutputStream(pathToProperties, CREATE)){
            Properties properties = new Properties();
            properties.setProperty("outDirPath", file.getPath());
            properties.store(output,null);
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private File getPersistedBrowseLocation() {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "image-exporter.properties" );

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                String outFilePath = properties.getProperty("outDirPath");
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
}

