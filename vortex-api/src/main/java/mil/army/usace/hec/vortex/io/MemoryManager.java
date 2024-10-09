package mil.army.usace.hec.vortex.io;

final class MemoryManager {
    private static final Runtime runtime = Runtime.getRuntime();
    private static final double DEFAULT_MEMORY_THRESHOLD = 0.8;

    private MemoryManager() {
        // Utility Class
    }

    static boolean isMemoryAvailable() {
        return (runtime.totalMemory() - runtime.freeMemory()) / (double) runtime.maxMemory() < DEFAULT_MEMORY_THRESHOLD;
    }
}
