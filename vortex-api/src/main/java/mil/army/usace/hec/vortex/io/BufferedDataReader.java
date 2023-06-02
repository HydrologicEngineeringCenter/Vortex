package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BufferedDataReader {
    private static final Logger logger = Logger.getLogger(BufferedDataReader.class.getName());

    private final String pathToFile;
    private final String variableName;

    private final List<VortexGrid> buffer = new ArrayList<>();
    private int bufferStartIndex = -1;
    private static final int MAX_BUFFER_SIZE = 10;

    public BufferedDataReader(String pathToFile, String variableName) {
        this.pathToFile = pathToFile;
        this.variableName = variableName;
    }

    /* Public Methods */
    public VortexGrid get(int index) {
        if (index < 0 || index > getCount()) {
            logger.warning("Out of range index");
            return null;
        }

        boolean isInBuffer = (index >= bufferStartIndex) && (index <= (bufferStartIndex + buffer.size()));
        if (!isInBuffer) loadBuffer(index);
        int bufferIndex = index - bufferStartIndex;
        return buffer.get(bufferIndex);
    }

    public int getCount() {
        DataReader dataReader = DataReader.builder()
                .path(pathToFile)
                .variable(variableName)
                .build();
        return dataReader.getDtoCount();
    }

    /* Utility Methods */
    private void loadBuffer(int index) {
        // Reset buffer
        buffer.clear();
        bufferStartIndex = index;

        // Load buffer
        DataReader dataReader = DataReader.builder()
                .path(pathToFile)
                .variable(variableName)
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
