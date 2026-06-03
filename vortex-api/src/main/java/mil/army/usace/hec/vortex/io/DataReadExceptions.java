package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexProperty;

import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for handling {@link DataReadException} consistently across
 * Vortex's batch tools. Kept separate from {@link DataReadException} so the
 * exception itself stays dependency-free (a pure carrier of error data) and
 * downstream consumers using a different logging or eventing stack can
 * ignore this class.
 */
public final class DataReadExceptions {

    private DataReadExceptions() {}

    /**
     * Standard reporting for a read failure caught at a batch-tool boundary:
     * log the exception at {@link Level#SEVERE} and fire a
     * {@link VortexProperty#ERROR} property change carrying the exception
     * message.
     */
    public static void reportTo(Logger logger, PropertyChangeSupport support, DataReadException e) {
        logger.log(Level.SEVERE, e, e::getMessage);
        support.firePropertyChange(VortexProperty.ERROR.toString(), null, e.getMessage());
    }
}
