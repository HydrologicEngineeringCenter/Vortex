package mil.army.usace.hec.vortex.io;

import java.util.Objects;

/**
 * Thrown when a {@link DataWriter} cannot write its data to its destination.
 *
 * <p>Unchecked because {@link DataWriter#write()} is declared without checked exceptions and is
 * dispatched by destination at build time, so a caller that batches writes has no more ability to
 * recover from one destination than from any other runtime failure. Every message names the
 * destination: an export that failed is only actionable if the caller can tell which file it
 * failed on.
 *
 * <p>Reporting a failure by logging it is not enough. A writer that logs and returns normally tells
 * the caller the data was written, and a caller has no way to notice that it was not.
 */
public final class DataWriteException extends RuntimeException {

    private final String destination;

    private DataWriteException(String destination, String reason, Throwable cause) {
        super(buildMessage(destination, reason), cause);
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    /**
     * @param destination the file the write was aimed at; always part of the message.
     * @param reason      what is wrong with it, and what the caller can do about it.
     */
    public static DataWriteException of(String destination, String reason) {
        return new DataWriteException(destination, reason, null);
    }

    public static DataWriteException of(String destination, String reason, Throwable cause) {
        return new DataWriteException(destination, reason, cause);
    }

    public String getDestination() {
        return destination;
    }

    private static String buildMessage(String destination, String reason) {
        return "Cannot write to " + destination + ": " + reason;
    }
}
