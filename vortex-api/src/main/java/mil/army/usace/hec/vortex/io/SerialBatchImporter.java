package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexGridCollection;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.geo.GeographicProcessor;
import mil.army.usace.hec.vortex.io.buffer.DataBuffer;
import mil.army.usace.hec.vortex.io.buffer.DataBufferConfig;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

class SerialBatchImporter extends BatchImporter {
    private static final Logger logger = Logger.getLogger(SerialBatchImporter.class.getName());

    // Netcdf Batch Importer
    SerialBatchImporter(Builder builder) {
        super(builder);
    }

    @Override
    public void process() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        // Geoprocessor
        GeographicProcessor geoProcessor = new GeographicProcessor(geoOptions);

        // Prepare NetCDF file to append
        List<DataReader> dataReaders = getDataReaders();
        NetcdfWriterPrep.initializeForAppend(destination.toString(), representativeCollection(dataReaders, geoOptions));

        // Buffered Data Writer
        DataBufferConfig config = DataBufferConfig.create().withAutoProcess();
        Consumer<List<VortexData>> writeFunction = data -> DataWriter.builder()
                .destination(destination)
                .options(writeOptions)
                .data(data)
                .build()
                .write();
        DataBuffer<VortexData> bufferedDataWriter = DataBuffer.of(DataBuffer.Type.MEMORY_DYNAMIC, config, writeFunction);

        // Buffered Read and Process
        dataReaders.parallelStream()
                .map(DataReader::getDtos)
                .flatMap(Collection::stream)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .map(geoProcessor::process)
                .forEachOrdered(bufferedDataWriter::add);

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

    private static VortexGridCollection representativeCollection(List<DataReader> readers, Map<String, String> geoOptions) {
        GeographicProcessor geoProcessor = new GeographicProcessor(geoOptions);
        List<VortexGrid> processedFirstGrids = readers.stream()
                .map(r -> r.getDto(0))
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .map(geoProcessor::process)
                .toList();
        VortexGridCollection vortexGridCollection = new VortexGridCollection(processedFirstGrids);
        List<VortexGrid> uniqueGrids = List.copyOf(vortexGridCollection.getRepresentativeGridNameMap().values());
        return new VortexGridCollection(uniqueGrids);
    }
}
