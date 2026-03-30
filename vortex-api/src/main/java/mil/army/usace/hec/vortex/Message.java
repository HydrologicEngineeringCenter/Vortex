package mil.army.usace.hec.vortex;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Message {
    private static final Properties PROPERTIES = loadProperties();

    private Message() {
    }

    public static String format(String key, Object... args) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) return key;
        return args.length == 0 ? value : MessageFormat.format(value, args);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = Message.class.getClassLoader().getResourceAsStream("message.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            Logger.getLogger(Message.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
        return properties;
    }
}