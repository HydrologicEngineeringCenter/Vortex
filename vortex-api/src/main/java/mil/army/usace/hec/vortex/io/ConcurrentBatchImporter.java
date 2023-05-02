package mil.army.usace.hec.vortex.io;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class ConcurrentBatchImporter extends BatchImporter {
    private static final Logger logger = Logger.getLogger(ConcurrentBatchImporter.class.getName());

    ConcurrentBatchImporter(Builder builder) {
        super(builder);
    }

    @Override
    public void process() {
        Instant start = Instant.now();

        List<ImportableUnit> importableUnits = getDataReaders().stream()
                .map(reader -> ImportableUnit.builder()
                        .reader(reader)
                        .geoOptions(geoOptions)
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build())
                .collect(Collectors.toList());

        int totalCount = importableUnits.size();

        importableUnits.parallelStream().forEach(importableUnit -> {
            importableUnit.addPropertyChangeListener(writeProgressListener(totalCount));
            importableUnit.process();
        });

        long seconds = Duration.between(start, Instant.now()).toSeconds();
        String timeMessage = String.format("Batch import time: %d:%02d:%02d%n", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
        logger.info(timeMessage);
    }
}
