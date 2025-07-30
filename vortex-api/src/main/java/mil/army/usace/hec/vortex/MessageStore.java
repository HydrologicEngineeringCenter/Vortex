package mil.army.usace.hec.vortex;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum MessageStore {
    INSTANCE;

    private final Properties properties;

    MessageStore() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("message.properties")) {
            properties.load(input);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static MessageStore getInstance() {
        return INSTANCE;
    }

    public String getMessage(String key) {
        return properties.getProperty(key);
    }
}
