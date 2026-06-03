package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexProperty;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DataReadExceptionsTest {

    @Test
    void reportToLogsSevereAndFiresErrorEvent() {
        Logger logger = Logger.getLogger("DataReadExceptionsTest.reportTo");
        logger.setUseParentHandlers(false);
        AtomicReference<LogRecord> captured = new AtomicReference<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { captured.set(record); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(handler);

        PropertyChangeSupport support = new PropertyChangeSupport(this);
        List<PropertyChangeEvent> events = new ArrayList<>();
        PropertyChangeListener listener = events::add;
        support.addPropertyChangeListener(listener);

        try {
            DataReadException e = DataReadException.ioError("/data.dss", "//A/", "boom");
            DataReadExceptions.reportTo(logger, support, e);

            assertNotNull(captured.get(), "expected a log record");
            assertEquals(Level.SEVERE, captured.get().getLevel());
            assertSame(e, captured.get().getThrown());

            assertEquals(1, events.size());
            assertEquals(VortexProperty.ERROR.toString(), events.get(0).getPropertyName());
            assertEquals("boom", events.get(0).getNewValue());
        } finally {
            logger.removeHandler(handler);
            support.removePropertyChangeListener(listener);
        }
    }
}
