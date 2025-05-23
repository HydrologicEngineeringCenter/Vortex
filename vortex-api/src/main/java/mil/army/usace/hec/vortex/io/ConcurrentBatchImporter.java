package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.util.List;
import java.util.logging.Logger;

class ConcurrentBatchImporter extends BatchImporter {
    private static final Logger logger = Logger.getLogger(ConcurrentBatchImporter.class.getName());

    ConcurrentBatchImporter(Builder builder) {
        super(builder);
    }

    @Override
    public void process() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        List<ImportableUnit> importableUnits = getImportableUnits();

        for (ImportableUnit importableUnit : importableUnits) {
            totalCount += importableUnit.getDtoCount();
        }

        importableUnits.parallelStream().forEach(importableUnit -> {
            importableUnit.addPropertyChangeListener(propertyChangeListener());
            importableUnit.process();
        });

        stopwatch.end();
        String timeMessage = "Batch import time: " + stopwatch;
        logger.info(timeMessage);

        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, null);
    }
}
