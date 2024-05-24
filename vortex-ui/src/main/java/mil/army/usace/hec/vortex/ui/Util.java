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
import java.util.*;
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

    public static void sortDssVariables(Set<String> dssVariables) {
        if(dssVariables == null || dssVariables.isEmpty()) { return; }

        List<String[]> datedRecords = new ArrayList<>();
        List<String[]> nonDatedRecords = new ArrayList<>();

        for (String dssPathname : dssVariables) {
            String[] split = dssPathname.split("/", -1);
            if (split[4].matches("[0-9]{2}[A-Za-z]{3}[0-9]{4}:[0-9]{4}")) {
                datedRecords.add(split);
            } else {
                nonDatedRecords.add(split);
            }
        }

        // Sort based on A/B/C/D/E/F part
        nonDatedRecords.sort(Comparator.comparing(s -> ((String[]) s)[1])
                .thenComparing(s -> ((String[]) s)[2])
                .thenComparing(s -> ((String[]) s)[3])
                .thenComparing(s -> ((String[]) s)[4])
                .thenComparing(s -> ((String[]) s)[5])
                .thenComparing(s -> ((String[]) s)[6])
        );

        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("ddMMMuuuu:HHmm")
                    .toFormatter(Locale.ENGLISH);

            // Sort based on C/D/A/B/F part
            datedRecords.sort(Comparator.comparing(s -> ((String[]) s)[3])
                    .thenComparing(s -> LocalDateTime.parse(((String[]) s)[4], formatter))
                    .thenComparing(s -> ((String[]) s)[1])
                    .thenComparing(s -> ((String[]) s)[2])
                    .thenComparing(s -> ((String[]) s)[6])
            );

        } catch (DateTimeParseException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        dssVariables.clear();

        for (String[] split : datedRecords) {
            String joined = String.join("/", split);
            dssVariables.add(joined);
        }

        for (String[] split : nonDatedRecords) {
            String joined = String.join("/", split);
            dssVariables.add(joined);
        }
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
