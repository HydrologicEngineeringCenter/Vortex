package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BufferedDataReader {
    private static final Logger logger = Logger.getLogger(BufferedDataReader.class.getName());

    private final String pathToFile;
    private final String pathToData;
    private final int numGrids;
    private final VortexDataType dataType;

    private final List<VortexGrid> buffer = new ArrayList<>();
    private int bufferStartIndex = -1;
    private static final int MAX_BUFFER_SIZE = 10;

    public BufferedDataReader(String pathToFile, String pathToData) {
        this.pathToFile = pathToFile;
        this.pathToData = pathToData;

        DataReader dataReader = DataReader.builder()
                .path(pathToFile)
                .variable(pathToData)
                .build();
        this.numGrids = dataReader.getDtoCount();
        this.dataType = get(0).dataType();
    }

    /* Public Methods */
    public List<VortexGrid> getAll() {
        DataReader dataReader = DataReader.builder()
                .path(pathToFile)
                .variable(pathToData)
                .build();
        return dataReader.getDtos().stream().map(VortexGrid.class::cast).collect(Collectors.toList());
    }

    public VortexGrid get(int index) {
        if (index < 0 || index >= getCount()) {
            logger.warning("Out of range index");
            return null;
        }

        boolean isInBuffer = (index >= bufferStartIndex) && (index < (bufferStartIndex + buffer.size()));
        if (!isInBuffer) loadBuffer(index);
        int bufferIndex = index - bufferStartIndex;
        return buffer.get(bufferIndex);
    }

    public int getCount() {
        return numGrids;
    }

    public VortexDataType getType() {
        return dataType;
    }

    public String getPathToFile() {
        return pathToFile;
    }

    public String getPathToData() {
        return pathToData;
    }

    /* Utility Methods */
    private void loadBuffer(int index) {
        // Reset buffer
        buffer.clear();
        bufferStartIndex = index;

        // Load buffer
        DataReader dataReader = DataReader.builder()
                .path(pathToFile)
                .variable(pathToData)
                .build();

        int maxDataIndex = getCount();
        int maxBufferIndex = index + MAX_BUFFER_SIZE;
        int endIndex = Math.min(maxBufferIndex, maxDataIndex);

        for (int i = index; i < endIndex; i++) {
            VortexData data = dataReader.getDto(i);
            buffer.add((VortexGrid) data);
        }
    }
}
