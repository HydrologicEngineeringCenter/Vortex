package mil.army.usace.hec.vortex.io;

    import mil.army.usace.hec.vortex.*;
import mil.army.usace.hec.vortex.geo.GeographicProcessor;
import mil.army.usace.hec.vortex.io.buffer.DataBuffer;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
    import java.util.Comparator;
    import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

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

        List<VortexTimeRecord> timeRecords = dataReaders.stream()
                .map(DataReader::getTimeRecords)
                .flatMap(Collection::stream)
                .distinct()
                .sorted(Comparator.comparing(VortexTimeRecord::startTime))
                .toList();

        NetcdfWriterPrep.initializeForAppend(destination.toString(), representativeCollection(dataReaders, geoOptions), timeRecords);

        // Buffered Data Writer
        DataBuffer<VortexData> bufferedDataWriter = DataBuffer.of(DataBuffer.Type.MEMORY_DYNAMIC);

        // Buffered Read and Process
        Consumer<Stream<VortexData>> bufferProcessFunction = stream -> writeBufferedData(destination, writeOptions, stream);
        dataReaders.parallelStream()
                .map(DataReader::getDtos)
                .flatMap(Collection::stream)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .map(geoProcessor::process)
                .forEachOrdered(grid -> bufferedDataWriter.addAndProcessWhenFull(grid, bufferProcessFunction, false));
        bufferedDataWriter.processBufferAndClear(bufferProcessFunction, true);

        stopwatch.end();
        String timeMessage = "Batch import time: " + stopwatch;
        logger.info(timeMessage);

        support.firePropertyChange(VortexProperty.STATUS, null, null);
    }

    private void writeBufferedData(Path destination, Map<String, String> writeOptions, Stream<VortexData> bufferedData) {
        List<VortexData> bufferedDataList = bufferedData.toList();

        List<VortexTimeRecord> x = bufferedDataList.stream()
                .map(VortexTimeRecord::of)
                .filter(r -> r.startTime().isAfter(ZonedDateTime.of(1951, 12, 31, 21, 0, 0, 0, ZoneId.of("UTC"))))
                .toList();

        if (!x.isEmpty()) {
            System.out.println("Here");
        }

        DataWriter dataWriter = DataWriter.builder()
                .destination(destination)
                .options(writeOptions)
                .data(bufferedDataList)
                .build();

        dataWriter.write();
        logger.info("Completed Buffered Write Count: " + bufferedDataList.size());
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
