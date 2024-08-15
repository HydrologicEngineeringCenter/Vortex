package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DSSPathname;
import mil.army.usace.hec.vortex.*;
import mil.army.usace.hec.vortex.geo.GeographicProcessor;
import mil.army.usace.hec.vortex.io.buffer.DataBuffer;
import mil.army.usace.hec.vortex.temporal.VortexTimeRecord;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

class SerialBatchImporter extends BatchImporter {
    private static final Logger logger = Logger.getLogger(SerialBatchImporter.class.getName());

    private final AtomicInteger totalDataCount = new AtomicInteger(0);
    private final AtomicInteger doneDataCount = new AtomicInteger(0);

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
        totalDataCount.set(dataReaders.stream().mapToInt(DataReader::getDtoCount).sum());

        Stream<VortexTimeRecord> sortedRecordStream = dataReaders.parallelStream()
                .map(DataReader::getTimeRecords)
                .flatMap(Collection::stream)
                .distinct()
                .sorted(Comparator.comparing(VortexTimeRecord::startTime));
        NetcdfWriterPrep.initializeForAppend(destination.toString(), representativeCollection(dataReaders, geoOptions), sortedRecordStream);

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
                .forEach(grid -> bufferedDataWriter.addAndProcessWhenFull(grid, bufferProcessFunction, false));
        bufferedDataWriter.processBufferAndClear(bufferProcessFunction, true);

        stopwatch.end();
        String timeMessage = "Batch import time: " + stopwatch;
        logger.info(timeMessage);

        support.firePropertyChange(VortexProperty.STATUS, null, null);
    }

    private void writeBufferedData(Path destination, Map<String, String> writeOptions, Stream<VortexData> bufferedData) {
        Comparator<VortexData> comparator = Comparator.comparing(VortexData::startTime).thenComparing(VortexData::endTime);
        List<VortexData> bufferedDataList = bufferedData.sorted(comparator).toList();

        DataWriter dataWriter = DataWriter.builder()
                .destination(destination)
                .options(writeOptions)
                .data(bufferedDataList)
                .build();

        dataWriter.write();

        // Update Progress
        double doneCount = doneDataCount.addAndGet(bufferedDataList.size());
        double totalCount = totalDataCount.addAndGet(bufferedDataList.size());
        int progress = (int) ((doneCount / totalCount) * 100);
        support.firePropertyChange(VortexProperty.PROGRESS, null, progress);
        logger.info(() -> "Completed Buffered Write Count: " + bufferedDataList.size());
    }

    private static VortexGridCollection representativeCollection(List<DataReader> readers, Map<String, String> geoOptions) {
        GeographicProcessor geoProcessor = new GeographicProcessor(geoOptions);
        Set<String> seenVariables = new HashSet<>();

        List<VortexGrid> processedFirstGrids = readers.stream()
                .filter(reader -> isUniqueVariableReader(seenVariables, reader))
                .map(r -> r.getDto(0))
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .map(geoProcessor::process)
                .toList();
        VortexGridCollection vortexGridCollection = new VortexGridCollection(processedFirstGrids);
        List<VortexGrid> uniqueGrids = List.copyOf(vortexGridCollection.getRepresentativeGridNameMap().values());
        return new VortexGridCollection(uniqueGrids);
    }

    private static boolean isUniqueVariableReader(Set<String> seenVariables, DataReader dataReader) {
        String pathToFile = dataReader.path;
        String variableName = dataReader.variableName;

        if (pathToFile.toLowerCase().endsWith(".dss")) {
            variableName = retrieveDssVariableName(variableName);
        }

        if (seenVariables.contains(variableName)) {
            return false;
        } else {
            seenVariables.add(variableName);
            return true;
        }
    }

    private static String retrieveDssVariableName(String variableName) {
        DSSPathname dssPathname = new DSSPathname(variableName);
        return dssPathname.cPart();
    }
}
