package mil.army.usace.hec.vortex.io;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class MemoryDynamicWriteDataBuffer<T> implements WriteDataBuffer<T> {
    private final Queue<T> buffer = new ConcurrentLinkedQueue<>();

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

    @Override
    public synchronized void processBufferAndClear(Consumer<Stream<T>> bufferProcessFunction, boolean isForced) {
        if (isFull() || isForced) {
            bufferProcessFunction.accept(getBufferAsStream());
            clear();
        }
    }

    @Override
    public synchronized void addAndProcessWhenFull(T data, Consumer<Stream<T>> bufferProcessFunction, boolean isForced) {
        if (isFull() || isForced) {
            processBufferAndClear(bufferProcessFunction, isForced);
        }

        add(data);
    }
}