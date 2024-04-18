package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexTimeRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BufferedDataReader {
    private static final Logger logger = Logger.getLogger(BufferedDataReader.class.getName());

    private final DataReader dataReader;
    private final int numGrids;
    private final VortexGrid baseGrid;

    private final List<VortexGrid> buffer = new ArrayList<>();
    private int bufferStartIndex = -1;
    private static final int MAX_BUFFER_SIZE = 10;

    public BufferedDataReader(String pathToFile, String pathToData) {
        dataReader = DataReader.builder()
                .path(pathToFile)
                .variable(pathToData)
                .build();
        this.numGrids = dataReader.getDtoCount();
        this.baseGrid = get(0);
    }

    /* Public Methods */
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

    /* Utility Methods */
    private void loadBuffer(int index) {
        // Reset buffer
        buffer.clear();
        bufferStartIndex = index;

        // Load buffer
        int maxDataIndex = getCount();
        int maxBufferIndex = index + MAX_BUFFER_SIZE;
        int endIndex = Math.min(maxBufferIndex, maxDataIndex);

        for (int i = index; i < endIndex; i++) {
            VortexData data = dataReader.getDto(i);
            buffer.add(data instanceof VortexGrid grid ? grid : null);
        }
    }

    public List<VortexTimeRecord> getTimeRecords() {
        return dataReader.getTimeRecords();
    }

    public VortexGrid getBaseGrid() {
        return baseGrid;
    }

    public String getPathToFile() {
        return dataReader.getPath();
    }

    public String getPathToData() {
        return dataReader.getVariableName();
    }
}
