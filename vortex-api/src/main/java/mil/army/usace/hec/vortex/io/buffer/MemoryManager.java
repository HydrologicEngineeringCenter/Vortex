package mil.army.usace.hec.vortex.io.buffer;

public final class MemoryManager {
    private static final Runtime runtime = Runtime.getRuntime();
    private static final double DEFAULT_MEMORY_THRESHOLD = 0.5;

    private MemoryManager() {
        // Utility Class
    }

    public static boolean isMemoryAvailable() {
        return (runtime.totalMemory() - runtime.freeMemory()) / (double) runtime.maxMemory() < DEFAULT_MEMORY_THRESHOLD;
    }
}
