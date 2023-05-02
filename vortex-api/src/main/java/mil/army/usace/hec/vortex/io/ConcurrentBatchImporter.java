package mil.army.usace.hec.vortex.io;

import java.util.List;
import java.util.stream.Collectors;

public class ConcurrentBatchImporter extends BatchImporter {

    public ConcurrentBatchImporter(Builder builder) {
        super(builder);
    }

    @Override
    void processWrite() {
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
    }
}
