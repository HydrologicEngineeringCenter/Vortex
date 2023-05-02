package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.GeographicProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SerialBatchImporter extends BatchImporter {
    private static final Logger logger = Logger.getLogger(SerialBatchImporter.class.getName());

    public SerialBatchImporter(Builder builder) {
        super(builder);
    }

    @Override
    public void process() {
        Instant start = Instant.now();

        GeographicProcessor geoProcessor = new GeographicProcessor(geoOptions);

        List<VortexData> vortexDataList = getDataReaders().parallelStream()
                .map(DataReader::getDtos)
                .flatMap(Collection::stream)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .map(geoProcessor::process)
                .collect(Collectors.toList());

        DataWriter writer = DataWriter.builder()
                .data(vortexDataList)
                .destination(destination)
                .options(writeOptions)
                .build();

        int totalCount = vortexDataList.size();
        writer.addListener(writeProgressListener(totalCount));
        writer.write();

        long seconds = Duration.between(start, Instant.now()).toSeconds();
        String timeMessage = String.format("Batch import time: %d:%02d:%02d%n", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
        logger.info(timeMessage);
    }
}
