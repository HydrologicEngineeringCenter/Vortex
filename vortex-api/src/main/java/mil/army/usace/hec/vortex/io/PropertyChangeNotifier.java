package mil.army.usace.hec.vortex.io;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Interface to enable objects to notify registered listeners about property changes.
 */
public interface PropertyChangeNotifier {
    PropertyChangeSupport getPropertyChangeSupport();

    default void addListener(PropertyChangeListener pcl) {
        PropertyChangeSupport support = getPropertyChangeSupport();
        support.addPropertyChangeListener(pcl);
    }

    default void removeListener(PropertyChangeListener pcl) {
        PropertyChangeSupport support = getPropertyChangeSupport();
        support.removePropertyChangeListener(pcl);
    }
}
