package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Message;
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

        String messageBegin = Message.format("import_begin", totalCount);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);

        importableUnits.parallelStream().forEach(importableUnit -> {
            importableUnit.addPropertyChangeListener(propertyChangeListener());
            importableUnit.process();
        });

        stopwatch.end();
        String timeMessage = "Batch import time: " + stopwatch;
        logger.info(timeMessage);

        String messageEnd = Message.format("import_end", totalCount, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String messageTime = Message.format("import_time", stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
    }
}
