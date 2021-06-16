package mil.army.usace.hec.vortex.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IconResources {
    public static final Logger logger = Logger.getLogger(IconResources.class.getName());

    private IconResources() {}

    public static Icon loadIcon(String key) {
        try {
            return new ImageIcon(ImageIO.read( ClassLoader.getSystemResource( key )));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
        return null;
    }

    public static Image loadImage(String key) {
        try {
            return ImageIO.read( ClassLoader.getSystemResource( key ));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
        return null;
    }
}
