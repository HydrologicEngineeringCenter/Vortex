package exporter.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import exporter.WizardData;
import javafx.beans.binding.When;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WizardController {

    private static final String CONTROLLER_KEY = "controller";

    @FXML
    VBox contentPanel;

    @FXML
    Button btnNext, btnBack, btnCancel;

    @FXML
    ButtonBar buttonBar;

    @Inject
    Injector injector;

    @Inject
    WizardData model;

    private final List<Parent> steps = new ArrayList<>();

    private final IntegerProperty currentStep = new SimpleIntegerProperty(-1);

    @FXML
    public void initialize() throws Exception {

        buildSteps();

        initButtons();

        setInitialContent();

    }

    private void initButtons() {
        btnBack = new Button("Back");
        btnBack.disableProperty().bind( currentStep.lessThanOrEqualTo(0)
                .or(currentStep.greaterThanOrEqualTo(steps.size()-2)) );
        btnBack.setOnAction(action -> back());

        btnNext = new Button("Next");
        btnNext.disableProperty().bind( currentStep.greaterThanOrEqualTo(steps.size()-2) );
        btnNext.setOnAction(action -> next());

        btnCancel = new Button();
        btnCancel.textProperty().bind(
                new When(
                        currentStep.lessThan(steps.size()-1)
                )
                        .then("Cancel")
                        .otherwise("Close")
        );
        btnCancel.setOnAction(action -> cancel());

        Insets insets = new Insets(10, 10, 10, 10);
        buttonBar.setPadding(insets);

        buttonBar.getButtons().add(btnBack);
        ButtonBar.setButtonData(btnBack, ButtonData.BACK_PREVIOUS);

        buttonBar.getButtons().add(btnNext);
        ButtonBar.setButtonData(btnNext, ButtonData.NEXT_FORWARD);

        buttonBar.getButtons().add(btnCancel);
        ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);
    }

    private void setInitialContent() {
        currentStep.set( 0 );  // first element

        contentPanel.getChildren().add( steps.get( currentStep.get() ));
    }

    private void buildSteps() throws java.io.IOException {

        final JavaFXBuilderFactory bf = new JavaFXBuilderFactory();

        final Callback<Class<?>, Object> cb = clazz -> injector.getInstance(clazz);

        FXMLLoader fxmlLoaderStep1 = new FXMLLoader( WizardController.class.getResource("/fxml/Step1.fxml"), null, bf, cb);
        Parent step1 = fxmlLoaderStep1.load( );
        step1.getProperties().put( CONTROLLER_KEY, fxmlLoaderStep1.getController() );

        FXMLLoader fxmlLoaderStep2 = new FXMLLoader( WizardController.class.getResource("/fxml/Step2.fxml"), null, bf, cb );
        Parent step2 = fxmlLoaderStep2.load();
        step2.getProperties().put( CONTROLLER_KEY, fxmlLoaderStep2.getController() );

        FXMLLoader fxmlLoaderProcessing = new FXMLLoader( WizardController.class.getResource("/fxml/Processing.fxml"), null, bf, cb);
        Parent processing = fxmlLoaderProcessing.load();
        processing.getProperties().put( CONTROLLER_KEY, fxmlLoaderProcessing.getController() );

        FXMLLoader fxmlLoaderCompleted = new FXMLLoader( WizardController.class.getResource("/fxml/Completed.fxml"), null, bf, cb);
        Parent completed = fxmlLoaderCompleted.load();
        completed.getProperties().put( CONTROLLER_KEY, fxmlLoaderCompleted.getController() );

        steps.addAll( Arrays.asList(step1, step2, processing, completed));
    }

    @FXML
    public void next() {

        Parent p = steps.get(currentStep.get());
        Object controller = p.getProperties().get(CONTROLLER_KEY);

        // validate
        Method v = getMethod( Validate.class, controller );
        if( v != null ) {
            try {
                Object retval = v.invoke(controller);
                if( retval != null && !((Boolean) retval)) {
                    return;
                }

            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        // submit
        Scene scene = contentPanel.getScene();
        scene.setCursor(Cursor.WAIT);
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                Method sub = getMethod( Submit.class, controller );
                if( sub != null ) {
                    try {
                        sub.invoke(controller);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                return null ;
            }
        };
        task.setOnSucceeded(e -> scene.setCursor(Cursor.DEFAULT));
        if( currentStep.get() == (steps.size()-3) ) {
            task.setOnSucceeded(e -> this.next());
        }
        new Thread(task).start();

        if( currentStep.get() < (steps.size()-1) ) {
            contentPanel.getChildren().remove( steps.get(currentStep.get()) );
            currentStep.set( currentStep.get() + 1 );
            if (currentStep.get() >= (steps.size()-2)){
                contentPanel.setAlignment(Pos.CENTER);
            } else {
                contentPanel.setAlignment(Pos.TOP_LEFT);
            }
            contentPanel.getChildren().add( steps.get(currentStep.get()) );
        }
    }

    @FXML
    public void back() {

        if( currentStep.get() > 0 ) {
            contentPanel.getChildren().remove( steps.get(currentStep.get()) );
            currentStep.set( currentStep.get() - 1 );
            if (currentStep.get() >= (steps.size()-2)){
                contentPanel.setAlignment(Pos.CENTER);
            } else {
                contentPanel.setAlignment(Pos.TOP_LEFT);
            }
            contentPanel.getChildren().add( steps.get(currentStep.get()) );
        }
    }

    @FXML
    public void cancel() {
        Stage stage = ((Stage) contentPanel.getScene().getWindow());
        stage.fireEvent(
                new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)
        );
    }

    private Method getMethod(Class<? extends Annotation> an, Object obj) {

        if( an == null ) {
            return null;
        }

        if( obj == null ) {
            return null;
        }

        Method[] methods = obj.getClass().getMethods();
        if( methods != null && methods.length > 0 ) {
            for( Method m : methods ) {
                if( m.isAnnotationPresent(an)) {
                    return m;
                }
            }
        }
        return null;
    }
}

