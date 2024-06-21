package mil.army.usace.hec.vortex.io.buffer;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface DataBuffer<T> {
    enum Type {MEMORY_DYNAMIC}
    
    void add(T data);
    void clear();
    boolean isFull();
    void processAllData();
    Stream<T> getBufferAsStream();

    default void addAndProcessWhenFull(T data, Consumer<Stream<T>> bufferProcessFunction) {
        if (isFull()) {
            bufferProcessFunction.accept(getBufferAsStream());
            clear();
        }

        add(data);
    }

    static <T> DataBuffer<T> of(Type type, DataBufferConfig configuration, Consumer<List<T>> writeFunction) {
        return switch (type) {
            case MEMORY_DYNAMIC -> new MemoryDynamicDataBuffer<>(configuration, writeFunction);
        };
    }
}
