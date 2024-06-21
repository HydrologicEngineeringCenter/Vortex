package mil.army.usace.hec.vortex.io.buffer;

public record DataBufferConfig(boolean autoProcess) {
    public static DataBufferConfig create() {
        return new DataBufferConfig(false);
    }

    public DataBufferConfig withAutoProcess() {
        return new DataBufferConfig(true);
    }
}
