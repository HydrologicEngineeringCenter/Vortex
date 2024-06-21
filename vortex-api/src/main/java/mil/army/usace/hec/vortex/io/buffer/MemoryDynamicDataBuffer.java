package mil.army.usace.hec.vortex.io.buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class MemoryDynamicDataBuffer<T> implements DataBuffer<T> {
    private final List<T> buffer = new ArrayList<>();
    private final DataBufferConfig configuration;
    private final Consumer<List<T>> writeFunction;

    MemoryDynamicDataBuffer(DataBufferConfig configuration, Consumer<List<T>> writeFunction) {
        this.configuration = configuration;
        this.writeFunction = writeFunction;
    }

    @Override
    public void add(T data) {
        if (configuration.autoProcess() && isFull()) {
            processAllData();
        }

        buffer.add(data);
    }

    @Override
    public boolean isFull() {
        return !MemoryManager.isMemoryAvailable();
    }

    @Override
    public void processAllData() {
        writeFunction.accept(buffer);
        buffer.clear();
    }
}