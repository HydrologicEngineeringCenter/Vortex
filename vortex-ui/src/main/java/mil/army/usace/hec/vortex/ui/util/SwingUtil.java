package mil.army.usace.hec.vortex.ui.util;

import javax.swing.*;
import java.awt.*;

public class SwingUtil {
    private SwingUtil(){}

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
