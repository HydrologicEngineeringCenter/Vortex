package mil.army.usace.hec.vortex.io.buffer;

import java.util.List;
import java.util.function.Consumer;

public interface DataBuffer<T> {
    enum Type {MEMORY_DYNAMIC}
    
    void add(T data);
    boolean isFull();
    void processAllData();

    static <T> DataBuffer<T> of(Type type, DataBufferConfig configuration, Consumer<List<T>> writeFunction) {
        return switch (type) {
            case MEMORY_DYNAMIC -> new MemoryDynamicDataBuffer<>(configuration, writeFunction);
        };
    }
}
