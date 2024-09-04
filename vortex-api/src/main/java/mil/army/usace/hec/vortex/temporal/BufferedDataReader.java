package mil.army.usace.hec.vortex.temporal;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.buffer.MemoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

final class BufferedDataReader {
    private static final Logger logger = Logger.getLogger(BufferedDataReader.class.getName());

    private final DataReader dataReader;
    private Integer numGrids;
    private VortexGrid baseGrid;

    private final List<VortexGrid> buffer = new ArrayList<>();
    private int bufferStartIndex = -1;
    private int currentBufferSize = 1; // Start with 1 item
    private int totalItemsRead = 0;
    private int readOperations = 0;

    BufferedDataReader(DataReader dataReader) {
        this.dataReader = dataReader;
    }

    /* Public Methods */
    public VortexGrid get(int index) {
        if (index < 0 || index >= getCount()) {
            logger.warning("Out of range index");
            return null;
        }

        boolean isInBuffer = (index >= bufferStartIndex) && (index < (bufferStartIndex + buffer.size()));
        if (!isInBuffer) {
            loadBuffer(index);
        }
        int bufferIndex = index - bufferStartIndex;
        return buffer.get(bufferIndex);
    }

    public int getCount() {
        if (numGrids == null) {
            numGrids = dataReader.getDtoCount();
        }

        return numGrids;
    }

    /* Utility Methods */
    private void loadBuffer(int index) {
        // Reset buffer
        buffer.clear();
        bufferStartIndex = index;

        // Load buffer
        int maxDataIndex = getCount();
        int endIndex = Math.min(index + currentBufferSize, maxDataIndex);

        for (int i = index; i < endIndex && MemoryManager.isMemoryAvailable(); i++) {
            VortexData data = dataReader.getDto(i);
            buffer.add(data instanceof VortexGrid grid ? grid : null);
            totalItemsRead++;
        }

        readOperations++;
        adjustBufferSize();
    }

    private void adjustBufferSize() {
        // Calculate average items per operation
        int averageItemCount = (readOperations > 0) ? (totalItemsRead / readOperations) : 1;

        // Adjust buffer size, increase gradually until reaching the average
        if (currentBufferSize < averageItemCount) {
            currentBufferSize *= 2;
        }
    }

    public List<VortexTimeRecord> getTimeRecords() {
        return dataReader.getTimeRecords();
    }

    public VortexGrid getBaseGrid() {
        if (baseGrid == null && getCount() > 0) {
            baseGrid = get(0);
        }

        return baseGrid;
    }
}