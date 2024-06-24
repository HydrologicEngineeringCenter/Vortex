package mil.army.usace.hec.vortex.io.buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class MemoryDynamicDataBuffer<T> implements DataBuffer<T> {
    private final List<T> buffer = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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
        return buffer.size() >= 50;
    }

    @Override
    public Stream<T> getBufferAsStream() {
        return buffer.stream();
    }

    @Override
    public synchronized void processBufferAndClear(Consumer<Stream<T>> bufferProcessFunction, boolean isForced) {
        if (isFull() || isForced) {
            bufferProcessFunction.accept(getBufferAsStream());
            clear();
        }
    }

    @Override
    public void addAndProcessWhenFull(T data, Consumer<Stream<T>> bufferProcessFunction, boolean isForced) {
        if (isFull() || isForced) {
            processBufferAndClear(bufferProcessFunction, isForced);
        }

        add(data);
    }
}