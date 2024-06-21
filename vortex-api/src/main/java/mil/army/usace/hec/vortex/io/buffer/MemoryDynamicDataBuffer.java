package mil.army.usace.hec.vortex.io.buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

final class MemoryDynamicDataBuffer<T> implements DataBuffer<T> {
    private final List<T> buffer = new ArrayList<>();

    @Override
    public void add(T data) {
        buffer.add(data);
    }

    @Override
    public void clear() {
        buffer.clear();
    }

    @Override
    public boolean isFull() {
        return !MemoryManager.isMemoryAvailable();
    }

    @Override
    public Stream<T> getBufferAsStream() {
        return buffer.stream();
    }
}