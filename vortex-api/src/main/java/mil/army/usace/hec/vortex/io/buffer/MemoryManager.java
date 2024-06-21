package mil.army.usace.hec.vortex.io.buffer;

public final class MemoryManager {
    private static final Runtime runtime = Runtime.getRuntime();
    private static final double MEMORY_THRESHOLD = 0.75;

    public static boolean isMemoryAvailable() {
        return (runtime.totalMemory() - runtime.freeMemory()) / (double) runtime.maxMemory() < MEMORY_THRESHOLD;
    }
}
