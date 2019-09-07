package exporter.controller;

import com.google.inject.Inject;
import exporter.WizardData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.io.TiffDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Step2Controller {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    VBox content;

    @Inject
    WizardData model;

    private boolean destinationDirInitialized = false;
    private Parent destinationDirView;
    private DestinationDirController destinationDirController;

    private boolean destinationFileInitialized = false;
    private Parent destinationFileView;
    private DestinationFileController destinationFileController;

    @FXML
    public void initialize() {
        model.selectedVariablesProperty().addListener((obs, oldText, newText) -> {
            if (model.getSelectedVariables().size() > 1) {
                content.getChildren().remove(destinationFileView);
                if (!destinationDirInitialized){
                    FXMLLoader fxmlLoader = new FXMLLoader( getClass().getResource("/fxml/DestinationDir.fxml"));
                    destinationDirView = null;
                    try {
                        destinationDirView = fxmlLoader.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    destinationDirController = fxmlLoader.getController();
                    destinationDirInitialized = true;
                } else {
                    content.getChildren().remove(destinationDirView);
                }

                File dir = destinationDirController.getPersistedBrowseLocation();
                String dest;
                if (dir != null && dir.exists()){
                    dest = dir.getPath();
                } else {
                    dest = System.getProperty("user.home");
                }
                destinationDirController.destination.textProperty().set(dest);

                content.getChildren().add(destinationDirView);
            } else {
                content.getChildren().remove(destinationDirView);
                if (!destinationFileInitialized){
                    FXMLLoader fxmlLoader = new FXMLLoader( getClass().getResource("/fxml/DestinationFile.fxml"));
                    destinationFileView = null;
                    try {
                        destinationFileView = fxmlLoader.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    destinationFileController = fxmlLoader.getController();
                    destinationFileInitialized = true;
                } else {
                    content.getChildren().remove(destinationFileView);
                }

                if (model.getSelectedVariables().size() == 1) {
                    List<VortexData> grids = DataReader.builder()
                            .path(Paths.get(model.getInFile()))
                            .variable(model.getSelectedVariables().get(0))
                            .build()
                            .getDTOs();

                    String defaultFileName = TiffDataWriter.autoGenerateFileName((VortexGrid) grids.get(0));
                    File dir = destinationFileController.getPersistedBrowseLocation();
                    String dest;
                    if (dir != null && dir.exists()){
                        dest = Paths.get(dir.toString(), defaultFileName).toString();
                    } else {
                        dest = Paths.get(System.getProperty("user.home"), defaultFileName).toString();
                    }
                    destinationFileController.destination.textProperty().set(dest);
                }

                content.getChildren().add(destinationFileView);
            }
        });
    }

    @Validate
    public boolean validate() {
        return true;
    }

    @Submit
    public void submit() {

        Path pathToSource = Paths.get(model.getInFile());
        Set<String> variables = new HashSet<>(model.getSelectedVariables());

        variables.forEach(variable -> {
            List<VortexData> grids = DataReader.builder()
                    .path(pathToSource)
                    .variable(variable)
                    .build()
                    .getDTOs();

            grids.forEach(grid -> {
                Path destination;
                if (model.getSelectedVariables().size() == 1 && grids.size() == 1){
                    destination = Paths.get(destinationFileController.getDestination());
                } else if (model.getSelectedVariables().size() == 1 && grids.size() != 1) {
                    String directory = Paths.get(destinationFileController.getDestination()).getParent().toString();
                    String fileNameIn = Paths.get(destinationFileController.getDestination()).getFileName().toString();
                    String fileNameInSansExt = fileNameIn.substring(0, fileNameIn.lastIndexOf('.'));
                    String fileName = TiffDataWriter.autoGenerateFileName(fileNameInSansExt, (VortexGrid) grid);
                    destination = Paths.get(directory, fileName);
                } else {
                    String directory = destinationDirController.getDestination();
                    String fileName = TiffDataWriter.autoGenerateFileName((VortexGrid) grid);
                    destination = Paths.get(directory, fileName);
                }
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
}

