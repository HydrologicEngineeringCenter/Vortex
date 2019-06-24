package converter;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mil.army.usace.hec.vortex.GdalRegister;

public class ConverterWizard extends Application {

    static {
        GdalRegister.getInstance();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        final Injector injector = Guice.createInjector( new WizardModule() );

        final Parent p = FXMLLoader.load( ConverterWizard.class.getResource("/fxml/Wizard.fxml"),
                null,
                new JavaFXBuilderFactory(),
                injector::getInstance
        );

        final Scene scene = new Scene(p);

        primaryStage.setScene( scene );
        primaryStage.setWidth( 600 );
        primaryStage.setHeight( 400 );
        primaryStage.setTitle("Grid To Point Converter Wizard");
        primaryStage.getIcons().add(new Image("/vortex_black.png"));
        primaryStage.initStyle(StageStyle.DECORATED);

        ViewSwitcher switcher = new ViewSwitcher();
        switcher.switchView(primaryStage);
    }

    public static void main(String[] args) { launch(args); }

}

