package mil.army.usace.hec.vortex.ui.util;

import javax.swing.*;
import java.awt.*;

public class SwingUtil {
    private SwingUtil(){}

    /**
     * Verifies if the given point is visible on the screen.
     *
     * @param   location     The given location on the screen.
     * @return           True if the location is on the screen, false otherwise.
     */
    public static boolean isLocationInScreenBounds(Point location) {

        // Check if the location is in the bounds of one of the graphics devices.
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] graphicsDevices = graphicsEnvironment.getScreenDevices();
        Rectangle graphicsConfigurationBounds = new Rectangle();

        // Iterate over the graphics devices.
        for (GraphicsDevice graphicsDevice : graphicsDevices) {

            // Get the bounds of the device.
            graphicsConfigurationBounds.setRect(graphicsDevice.getDefaultConfiguration().getBounds());

            // Is the location in this bounds?
            graphicsConfigurationBounds.setRect(graphicsConfigurationBounds.x, graphicsConfigurationBounds.y,
                    graphicsConfigurationBounds.width, graphicsConfigurationBounds.height);
            if (graphicsConfigurationBounds.contains(location.x, location.y)) {

                // The location is in this screengraphics.
                return true;
            }

        }

        // We could not find a device that contains the given point.
        return false;
    }

    public static void setButtonSize(JButton button, Component component) {
        /* Set the button's size to component (such as HmsComboBox)'s preferred size */
        int max = (int) component.getMaximumSize().getHeight();
        int min = (int) component.getMinimumSize().getHeight();
        int pref = (int) component.getPreferredSize().getHeight();
        button.setMaximumSize(new Dimension(max, max));
        button.setMinimumSize(new Dimension(min, min));
        button.setPreferredSize(new Dimension(pref, pref));
        int size = (int) component.getSize().getHeight();
        button.setSize(size, size);
    }
}
