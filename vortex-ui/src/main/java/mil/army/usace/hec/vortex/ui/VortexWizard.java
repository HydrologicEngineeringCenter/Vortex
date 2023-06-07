package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.ui.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.CREATE;

public abstract class VortexWizard extends JFrame {

    private final Path pathToProperties = Paths.get(Util.getCacheDir() + File.separator + "VortexWizard.properties");

    protected VortexWizard() {
        initComponentListener();
        initWindowListener();
    }
    public abstract void buildAndShowUI();

    private void initComponentListener() {
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent componentEvent) {
                setPersistedProperties();
            }

            @Override
            public void componentMoved(ComponentEvent componentEvent) {
                setPersistedProperties();
            }
        });
    }

    private void initWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setPersistedProperties();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                setPersistedProperties();
            }
        });
    }

    void setPersistedProperties() {
        Logger logger = Logger.getLogger(this.getClass().getName());

        try {
            Files.createDirectories(pathToProperties.getParent());
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        try (OutputStream output = Files.newOutputStream(pathToProperties, CREATE)) {
            Properties properties = new Properties();
            properties.setProperty("x", String.valueOf(getX()));
            properties.setProperty("y", String.valueOf(getY()));
            properties.setProperty("w", String.valueOf(getWidth()));
            properties.setProperty("h", String.valueOf(getHeight()));
            properties.store(output, null);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
    }

    Point getPersistedLocation() {
        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                int x = Integer.parseInt(properties.getProperty("x"));
                int y = Integer.parseInt(properties.getProperty("y"));
                Point point = new Point(x, y);
                if (SwingUtil.isLocationInScreenBounds(point)) {
                    return point;
                } else {
                    return new Point();
                }
            } catch (IOException | NumberFormatException e) {
                return new Point();
            }
        }
        return new Point();
    }

    Dimension getPersistedSize() {
        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                int w = Integer.parseInt(properties.getProperty("w"));
                int h = Integer.parseInt(properties.getProperty("h"));
                return new Dimension(w, h);
            } catch (IOException | NumberFormatException e) {
                return new Dimension(600, 400);
            }
        }
        return new Dimension(600, 400);
    }
}
