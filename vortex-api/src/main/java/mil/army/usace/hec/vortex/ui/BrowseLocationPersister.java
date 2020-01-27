package mil.army.usace.hec.vortex.ui;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.CREATE;

public interface BrowseLocationPersister {

    default void setPersistedBrowseLocation(File file) {
        String className = this.getClass().getName();

        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + className + ".properties" );

        if(Files.notExists(pathToProperties.getParent())){
            try {
                Files.createDirectory(pathToProperties.getParent());
            } catch (IOException e) {
                LoggerFactory.getLogger(BrowseLocationPersister.class).error(e.toString());
            }
        }

        try(OutputStream output = Files.newOutputStream(pathToProperties, CREATE)){
            Properties properties = new Properties();
            properties.setProperty("browse_location", file.getPath());
            properties.store(output,null);
        } catch (IOException e) {
            LoggerFactory.getLogger(BrowseLocationPersister.class).error(e.toString());
        }
    }

    default File getPersistedBrowseLocation() {
        String className = this.getClass().getName();

        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + className + ".properties" );

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                String outFilePath = properties.getProperty("browse_location");
                if (outFilePath == null){
                    return null;
                }
                if (Files.exists(Paths.get(outFilePath))) {
                    return new File(outFilePath);
                }
                return null;
            } catch (IOException e) {
                LoggerFactory.getLogger(BrowseLocationPersister.class).error(e.toString());
                return null;
            }
        }
        return null;
    }
}
