package mil.army.usace.hec.vortex.io.temporal;

/**
 * Closed-open, [), interval on the integer number line.
 */
public interface Interval extends Comparable<Interval> {

    /**
     * Returns the starting point of this.
     */
    long start();

    /**
     * Returns the ending point of this.
     * <p>
     * The interval does not include this point.
     */
    long end();

    /**
     * Returns the length of this.
     */
    default long length() {
        return end() - start();
    }

    /**
     * Returns if this interval is adjacent to the specified interval.
     * <p>
     * Two intervals are adjacent if either one ends where the other starts.
     * @param other - the interval to compare this one to
     * @return if this interval is adjacent to the specified interval.
     */
    default boolean isAdjacent(Interval other) {
        return start() == other.end() || end() == other.start();
    }

    default boolean overlaps(Interval o) {
        return end() > o.start() && o.end() > start();
    }

    default int compareTo(Interval o) {
        if (start() > o.start()) {
            return 1;
        } else if (start() < o.start()) {
            return -1;
        } else if (end() > o.end()) {
            return 1;
        } else if (end() < o.end()) {
            return -1;
        } else {
            return 0;
        }
    }
}