package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.GeographicProcessor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SerialBatchImporter extends BatchImporter {

    public SerialBatchImporter(Builder builder) {
        super(builder);
    }

    @Override
    void processWrite() {
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
    }
}
