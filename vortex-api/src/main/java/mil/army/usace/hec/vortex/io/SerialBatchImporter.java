package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.VortexVariable;
import mil.army.usace.hec.vortex.geo.GeographicProcessor;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

class SerialBatchImporter extends BatchImporter {
    private static final Logger logger = Logger.getLogger(SerialBatchImporter.class.getName());

    SerialBatchImporter(Builder builder) {
        super(builder);
    }

    @Override
    public void process() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        List<VortexData> firstProcessedGridsFromReaders = firstProcessedDataFromReaders(getDataReaders(), geoOptions);

        DataWriter dataWriter = DataWriter.builder()
                .data(firstProcessedGridsFromReaders)
                .destination(destination)
                .options(writeOptions)
                .build();

        dataWriter.write();

        List<ImportableUnit> importableUnits = getImportableUnits();

        for (ImportableUnit importableUnit : importableUnits) {
            totalCount += importableUnit.getDtoCount();
        }

        importableUnits.forEach(importableUnit -> {
            importableUnit.addPropertyChangeListener(propertyChangeListener());
            importableUnit.process();
        });

        stopwatch.end();
        String timeMessage = "Batch import time: " + stopwatch;
        logger.info(timeMessage);

        support.firePropertyChange(VortexProperty.STATUS, null, null);
    }

    private static List<VortexData> firstProcessedDataFromReaders(List<DataReader> readers, Map<String, String> geoOptions) {
        GeographicProcessor geoProcessor = new GeographicProcessor(geoOptions);
        return readers.stream()
                .map(r -> r.getDto(0))
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .map(geoProcessor::process)
                .map(VortexData.class::cast)
                .toList();
    }
}
