package importer.controller;

import java.util.TreeSet;

import importer.WizardData;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.StageStyle;

public class Step2Controller {

    private Logger log = LoggerFactory.getLogger(Step2Controller.class);

    @FXML
    ListView<String> availableVariables, selectedVariables;

    @FXML
    Button addButton, removeButton;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        Image addImage = new Image(getClass().getResourceAsStream("/right-arrow-24.png"));
        addButton.setGraphic(new ImageView(addImage));

        Image removeImage = new Image(getClass().getResourceAsStream("/left-arrow-24.png"));
        removeButton.setGraphic(new ImageView(removeImage));

        availableVariables.itemsProperty().bindBidirectional(model.availableVariablesProperty());
        availableVariables.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableVariables.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                ObservableList<String> selection = availableVariables.getSelectionModel().getSelectedItems();
                TreeSet<String> selected = new TreeSet<>(this.selectedVariables.getItems());
                selected.addAll(selection);
                this.selectedVariables.setItems(FXCollections.observableArrayList(selected));
                model.removeAvailableVariables(selection);
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
                model.removeSelectedVariables(selection);
                selectedVariables.getSelectionModel().clearSelection();
            }
        });
    }

    @FXML
    private void handleAdd() {
        ObservableList<String> selection = availableVariables.getSelectionModel().getSelectedItems();
        TreeSet<String> selected = new TreeSet<>(this.selectedVariables.getItems());
        selected.addAll(selection);
        this.selectedVariables.setItems(FXCollections.observableArrayList(selected));
        model.removeAvailableVariables(selection);
        availableVariables.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRemove() {
        ObservableList<String> selection = selectedVariables.getSelectionModel().getSelectedItems();
        TreeSet<String> available = new TreeSet<>(this.availableVariables.getItems());
        available.addAll(selection);
        this.availableVariables.setItems(FXCollections.observableArrayList(available));
        model.removeSelectedVariables(selection);
        selectedVariables.getSelectionModel().clearSelection();
    }

    @Validate
    public boolean validate() throws Exception {

        if( selectedVariables.getItems().size() == 0) {
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
    public void submit() throws Exception {
        if( log.isDebugEnabled() ) {
            log.debug("[SUBMIT] the user has completed step 2");
        }
    }
}
