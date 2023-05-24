package mil.army.usace.hec.vortex.ui;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
    private static final Logger logger = Logger.getLogger(Util.class.getName());

    public static String getCacheDir() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            String userHomeDir = System.getProperty("user.home");
            return userHomeDir
                    + File.separator
                    + "AppData"
                    + File.separator
                    + "Roaming"
                    + File.separator
                    + "HEC"
                    + File.separator
                    + "HEC-HMS";
        } else if (osName.contains("mac")) {
            String userHomeDir = System.getProperty("user.home");
            return userHomeDir
                    + File.separator
                    + "Library"
                    + File.separator
                    + "Application Support"
                    + File.separator
                    + "HEC"
                    + File.separator
                    + "HEC-HMS";
        } else {
            String userHomeDir = System.getProperty("user.home");
            return userHomeDir
                    + File.separator
                    + ".hec"
                    + File.separator
                    + "hec-hms";
        }
    }

    public static String getOldCacheDir() {
        return System.getProperty("user.home") + File.separator + ".hms";
    }

    public static void migrateFromOldCacheDir(String filename) {
        Path newFile = Paths.get(Util.getCacheDir() + File.separator + filename);
        Path oldFile = Paths.get(Util.getOldCacheDir() + File.separator + filename);
        if(Files.exists(newFile) || !Files.exists(oldFile)) return;
        try {Files.move(oldFile, newFile);}
        catch(IOException e) {logger.log(Level.WARNING, e.getMessage());}
    }

    public static List<String> sortDssVariables(List<String> dssVariables) {
        if(dssVariables == null || dssVariables.isEmpty()) { return null; }

        List<String> datedRecords = new ArrayList<>();
        List<String> nonDatedRecords = new ArrayList<>();

        for (String dssPathname : dssVariables) {
            String[] split = dssPathname.split("/", -1);
            if (split[4].matches("[0-9]{2}[A-Za-z]{3}[0-9]{4}:[0-9]{4}")) {
                datedRecords.add(dssPathname);
            } else {
                nonDatedRecords.add(dssPathname);
            }
        }

        // Sort based on A part
        nonDatedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[1]));
        // Sort based on B part
        nonDatedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[2]));
        // Sort based on C part
        nonDatedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[3]));
        // Sort based on D part
        nonDatedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[5]));
        // Sort based on E part
        nonDatedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[5]));
        // Sort based on F part
        nonDatedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[6]));

        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("ddMMMuuuu:HHmm")
                    .toFormatter();

            // Sort based on D part
            datedRecords.sort(Comparator.comparing(s -> LocalDateTime.parse(s.split("/")[4], formatter)));
            // Sort based on A part
            datedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[1]));
            // Sort based on B part
            datedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[2]));
            // Sort based on C part
            datedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[3]));
            // Sort based on F part
            datedRecords.sort(Comparator.comparing(s -> s.split("/", -1)[6]));

        } catch (DateTimeParseException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        dssVariables.clear();
        dssVariables.addAll(datedRecords);
        dssVariables.addAll(nonDatedRecords);

        return dssVariables;
    }

    public static DefaultListModel<String> getDefaultListModel(JList<String> list) {
        ListModel<String> listModel = list.getModel();
        if(!(listModel instanceof DefaultListModel)) {
            logger.log(Level.SEVERE, list.getName() + " may have not been initialized");
            return null;
        } // If: listModel is not a DefaultListModel -- should not be happening

        return (DefaultListModel<String>) listModel;
    }
}
