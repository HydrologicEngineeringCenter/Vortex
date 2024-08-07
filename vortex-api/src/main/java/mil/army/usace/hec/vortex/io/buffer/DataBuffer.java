package mil.army.usace.hec.vortex.io.buffer;

import java.util.function.Consumer;
import java.util.stream.Stream;

public interface DataBuffer<T> {
    enum Type {MEMORY_DYNAMIC}
    
    void add(T data);
    void clear();
    boolean isFull();
    Stream<T> getBufferAsStream();

    default void processBufferAndClear(Consumer<Stream<T>> bufferProcessFunction, boolean isForced) {
        bufferProcessFunction.accept(getBufferAsStream());
        clear();
    }

    default void addAndProcessWhenFull(T data, Consumer<Stream<T>> bufferProcessFunction, boolean isForced) {
        if (isFull() || isForced) {
            processBufferAndClear(bufferProcessFunction, isForced);
        }

        add(data);
    }

    static <T> DataBuffer<T> of(Type type) {
        return switch (type) {
            case MEMORY_DYNAMIC -> new MemoryDynamicDataBuffer<>();
        };
    }
}
