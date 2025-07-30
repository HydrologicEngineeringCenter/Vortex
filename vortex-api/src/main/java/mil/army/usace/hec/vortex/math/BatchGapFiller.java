package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchGapFiller implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(BatchGapFiller.class.getName());
    private static final PathMatcher DSS_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**/*.{dss,DSS}");

    final String source;
    final Set<String> variables;
    final String destination;
    final Map<String, String> writeOptions;
    private final GapFillMethod method;

    final PropertyChangeSupport support = new PropertyChangeSupport(this);

    BatchGapFiller(Builder builder) {
        source = builder.source;
        variables = builder.variables;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
        method = builder.method;
    }

    public static class Builder {
        private String source;
        private Set<String> variables = new HashSet<>();
        boolean isSelectAll;
        private String destination;
        private final Map<String, String> writeOptions = new HashMap<>();
        private GapFillMethod method;

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder variables(List<String> variables) {
            this.variables.clear();
            this.variables.addAll(variables);
            return this;
        }

        public Builder selectAllVariables() {
            this.isSelectAll = true;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder writeOptions(Map<String, String> writeOptions) {
            this.writeOptions.putAll(writeOptions);
            return this;
        }

        public Builder method(GapFillMethod method) {
            this.method = method;
            return this;
        }

        public BatchGapFiller build() {
            if (isSelectAll) {
                variables = new HashSet<>();
                variables.addAll(DataReader.getVariables(source));
            }

            if (method == GapFillMethod.LINEAR_INTERPOLATION)
                return new LinearInterpGapFiller(this);

            if (method == GapFillMethod.TIME_STEP)
                return new TimeStepFiller(this);

            return new BatchGapFiller(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void run() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        String templateBegin = MessageStore.getInstance().getMessage("gap_filler_begin");
        String messageBegin = String.format(templateBegin);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);

        Set<String> datasetVars = DataReader.getVariables(source);

        AtomicInteger processed = new AtomicInteger();

        for (String variable : variables) {
            if (datasetVars.contains(variable)) {

                GapFiller gapFiller = GapFiller.of(method);

                try (DataReader reader = DataReader.builder()
                        .path(source)
                        .variable(variable)
                        .build()) {

                    int count = reader.getDtoCount();

                    for (int i = 0; i < count; i++) {
                        VortexGrid grid = (VortexGrid) reader.getDto(i);

                        List<VortexData> filled = List.of(gapFiller.fill(grid));

                        DataWriter writer = DataWriter.builder()
                                .data(filled)
                                .destination(destination)
                                .options(writeOptions)
                                .build();

                        writer.write();
                        processed.incrementAndGet();
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e, e::getMessage);
                }
            }
        }

        // The total count happens while processing for computational efficiency
        support.firePropertyChange("progress", null, 100);

        stopwatch.end();
        String timeMessage = "Batch gap-filler time: " + stopwatch;
        LOGGER.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("gap_filler_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("gap_filler_time");
        String messageTime = String.format(templateTime, stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
    }

    void condenseVariables() {
        if (DSS_MATCHER.matches(Path.of(source))) {
            Set<String> condensed = new HashSet<>();

            for (String dssPathname : variables) {
                String pathnameSansTime = removeTime(dssPathname);
                condensed.add(pathnameSansTime);
            }

            variables.clear();
            variables.addAll(condensed);
        }
    }

    private static String removeTime(String dssPathname) {
        if (dssPathname == null) {
            return null;
        }

        dssPathname = dssPathname.trim();

        // Check if pathname starts and ends with '/'
        if (!dssPathname.startsWith("/") || !dssPathname.endsWith("/")) {
            return dssPathname;
        }

        // Remove the leading and trailing '/'
        String trimmedPathname = dssPathname.substring(1, dssPathname.length() - 1);

        // Split the pathname into parts using '/' as delimiter, include empty strings
        // Also trim the parts
        String[] parts = Arrays.stream(trimmedPathname.split("/", -1))
                .map(String::trim)
                .toArray(String[]::new);

        // There should be exactly 6 parts
        if (parts.length != 6) {
            return dssPathname;
        }

        // Create and return a new DssPathname instance
        return "/" + String.join("/", parts[0], parts[1], parts[2], "*", "*", parts[5]) + "/";
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
}
