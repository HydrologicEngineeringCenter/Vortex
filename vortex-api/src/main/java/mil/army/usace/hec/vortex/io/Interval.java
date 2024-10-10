package mil.army.usace.hec.vortex.io;

/**
 * Closed-open, [), interval on the integer number line.
 */
interface Interval extends Comparable<Interval> {

    /**
     * Returns the starting point of this.
     */
    long startEpochSecond();

    /**
     * Returns the ending point of this.
     * <p>
     * The interval does not include this point.
     */
    long endEpochSecond();

    default boolean overlaps(Interval o) {
        return endEpochSecond() > o.startEpochSecond() && o.endEpochSecond() > startEpochSecond();
    }

    default int compareTo(Interval o) {
        if (startEpochSecond() > o.startEpochSecond()) {
            return 1;
        } else if (startEpochSecond() < o.startEpochSecond()) {
            return -1;
        } else {
            return Long.compare(endEpochSecond(), o.endEpochSecond());
        }
    }
}