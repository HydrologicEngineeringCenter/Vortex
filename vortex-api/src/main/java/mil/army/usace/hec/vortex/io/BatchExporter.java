package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.util.ImageUtils;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class BatchExporter implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(BatchExporter.class.getName());

    private final String pathToSource;
    private final Set<String> variables;
    private final String filenamePrefix;
    private final String destinationDir;
    private final ImageFileType imageFileType;

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private BatchExporter(Builder builder) {
        pathToSource = builder.pathToSource;
        variables = builder.variables;
        filenamePrefix = builder.filenamePrefix;
        destinationDir = builder.destinationDir;
        imageFileType = builder.imageFileType;
    }

    public static class Builder {
        private String pathToSource;
        private Set<String> variables;
        private String filenamePrefix;
        private String destinationDir;
        private ImageFileType imageFileType;

        public Builder() {
            // Default constructor
        }

        public Builder pathToSource(String pathToSource) {
            this.pathToSource = pathToSource;
            return this;
        }

        public Builder variables(Set<String> variables) {
            this.variables = variables;
            return this;
        }

        public Builder filenamePrefix(String filenamePrefix) {
            this.filenamePrefix = filenamePrefix;
            return this;
        }

        public Builder destinationDir(String destinationDir) {
            this.destinationDir = destinationDir;
            return this;
        }

        public Builder imageFileType(ImageFileType imageFileType) {
            this.imageFileType = imageFileType;
            return this;
        }

        public BatchExporter build() {
            return new BatchExporter(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void run() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        int totalCount = 0;
        List<DataReader> dataReaders = new ArrayList<>();
        for (String variable : variables) {
            DataReader dataReader = DataReader.builder()
                    .path(pathToSource)
                    .variable(variable)
                    .build();

            dataReaders.add(dataReader);

            totalCount += dataReader.getDtoCount();
        }

        AtomicInteger processed = new AtomicInteger();

        String templateBegin = MessageStore.getInstance().getMessage("exporter_begin");
        String messageBegin = String.format(templateBegin, totalCount);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);

        for (DataReader dataReader : dataReaders) {
            for (VortexData data : dataReader.getDtos()) {
                VortexGrid grid = (VortexGrid) data;
                String fileName = ImageUtils.generateFileName(filenamePrefix, grid, imageFileType);
                Path destination = Paths.get(destinationDir, fileName);

                DataWriter writer = DataWriter.builder()
                        .data(Collections.singletonList(grid))
                        .destination(destination)
                        .build();

                writer.write();

                int newValue = (int) (((float) processed.incrementAndGet() / totalCount) * 100);
                support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, newValue);
            }
        }

        stopwatch.end();
        String timeMessage = "Batch export time: " + stopwatch;
        LOGGER.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("exporter_end");
        String messageEnd = String.format(templateEnd, processed, destinationDir);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("exporter_time");
        String messageTime = String.format(templateTime, stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}
