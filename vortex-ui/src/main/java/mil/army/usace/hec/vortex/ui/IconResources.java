package mil.army.usace.hec.vortex.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IconResources {
    public static final Logger logger = Logger.getLogger(IconResources.class.getName());

    private IconResources() {}

    public static Icon loadIcon(String key) {
        try {
            setCacheDir();
            return new ImageIcon(ImageIO.read( ClassLoader.getSystemResource( key )));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
        return null;
    }

    public static Image loadImage(String key) {
        try {
            setCacheDir();
            return ImageIO.read( ClassLoader.getSystemResource( key ));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
        return null;
    }

    public static void setCacheDir() throws IOException {
        Path vortexHome = Paths.get(System.getProperty("user.home") + File.separator + ".vortex");
        Files.createDirectories(vortexHome);
        ImageIO.setCacheDirectory(vortexHome.toFile());
    }
}
