package mil.army.usace.hec.vortex.io;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The record axis of a NetCDF destination: the time steps it holds, in the order it holds them, and
 * the unit its time variable encodes them with.
 *
 * <p>A record is identified by its bounds quantized to that unit, because quantized is how they are
 * stored. Comparing in the stored space is what lets a time step that is already in the file resolve
 * to the index it already occupies instead of being appended a second time.
 *
 * <p>Records can only be added at the end of an unlimited dimension, so this axis grows by appending
 * and never by inserting. A destination written out of chronological order therefore holds its
 * records in the order they were written, which is the price of appending to a file that is already
 * on disk.
 */
final class NetcdfTimeAxis {
    static final String TIME_BOUNDS_NAME = "time_bnds";

    private final ChronoUnit unit;
    private final boolean hasBounds;
    private final List<VortexDataInterval> records;
    private final Map<VortexDataInterval, Integer> recordIndex;

    /**
     * The outcome of offering a batch of records to an axis: the axis the write has to index into, and
     * the records the destination does not hold yet and that have to be written to it.
     */
    record Merge(NetcdfTimeAxis axis, List<VortexDataInterval> newRecords) {
    }

    private NetcdfTimeAxis(ChronoUnit unit, boolean hasBounds, List<VortexDataInterval> records) {
        this.unit = unit;
        this.hasBounds = hasBounds;
        this.records = List.copyOf(records);
        this.recordIndex = new HashMap<>();
        for (int index = 0; index < this.records.size(); index++) {
            this.recordIndex.putIfAbsent(this.records.get(index), index);
        }
    }

    /* Factory */
    static NetcdfTimeAxis of(ChronoUnit unit, boolean hasBounds, Collection<VortexDataInterval> records) {
        List<VortexDataInterval> stored = new ArrayList<>();
        Set<VortexDataInterval> seen = new HashSet<>();
        for (VortexDataInterval record : records) {
            VortexDataInterval quantized = quantize(record, unit);
            if (quantized != null && seen.add(quantized)) {
                stored.add(quantized);
            }
        }
        return new NetcdfTimeAxis(unit, hasBounds, stored);
    }

    /**
     * Reads the axis a destination already holds. The time variable's units attribute defines the
     * encoding every later write to that file has to use, whether or not it agrees with the units the
     * batch asking to be appended would have chosen.
     */
    static NetcdfTimeAxis readFrom(String destination, NetcdfFile file) {
        Variable timeVariable = file.findVariable(CF.TIME);
        if (timeVariable == null) {
            throw DataWriteException.of(destination, "it has no '" + CF.TIME + "' variable");
        }

        ChronoUnit unit = readTimeUnit(destination, timeVariable);
        Variable boundsVariable = file.findVariable(TIME_BOUNDS_NAME);

        try {
            if (boundsVariable != null) {
                return readBounds(destination, boundsVariable, unit);
            }

            long[] midTimes = readLongs(timeVariable);
            List<VortexDataInterval> records = new ArrayList<>();
            for (long midTime : midTimes) {
                ZonedDateTime time = fromStored(midTime, unit);
                records.add(VortexDataInterval.of(time, time));
            }
            return new NetcdfTimeAxis(unit, false, records);
        } catch (IOException | InvalidRangeException e) {
            throw DataWriteException.of(destination, "its time axis cannot be read (" + e.getMessage() + ")", e);
        }
    }

    private static NetcdfTimeAxis readBounds(String destination, Variable boundsVariable, ChronoUnit unit)
            throws IOException, InvalidRangeException {
        long[] bounds = readLongs(boundsVariable);
        if (bounds.length % 2 != 0) {
            throw DataWriteException.of(destination,
                    "its '" + TIME_BOUNDS_NAME + "' variable does not hold a start and an end for every record");
        }

        List<VortexDataInterval> records = new ArrayList<>();
        for (int index = 0; index + 1 < bounds.length; index += 2) {
            records.add(VortexDataInterval.of(fromStored(bounds[index], unit), fromStored(bounds[index + 1], unit)));
        }
        return new NetcdfTimeAxis(unit, true, records);
    }

    /* Merge */
    Merge mergedWith(Collection<VortexDataInterval> batchRecords) {
        List<VortexDataInterval> merged = new ArrayList<>(records);
        List<VortexDataInterval> newRecords = new ArrayList<>();
        Set<VortexDataInterval> seen = new HashSet<>(recordIndex.keySet());

        for (VortexDataInterval batchRecord : batchRecords) {
            VortexDataInterval quantized = quantize(batchRecord, unit);
            if (quantized != null && seen.add(quantized)) {
                newRecords.add(quantized);
                merged.add(quantized);
            }
        }

        return new Merge(new NetcdfTimeAxis(unit, hasBounds, merged), newRecords);
    }

    /* Lookup */
    int indexOf(VortexDataInterval record) {
        VortexDataInterval quantized = quantize(record, unit);
        return quantized == null ? -1 : recordIndex.getOrDefault(quantized, -1);
    }

    int recordCount() {
        return records.size();
    }

    List<VortexDataInterval> records() {
        return records;
    }

    boolean hasBounds() {
        return hasBounds;
    }

    ChronoUnit unit() {
        return unit;
    }

    /* Encoding */
    long[] midTimes(List<VortexDataInterval> records) {
        long[] midTimes = new long[records.size()];
        for (int index = 0; index < records.size(); index++) {
            VortexDataInterval record = records.get(index);
            midTimes[index] = (toStored(record.startTime()) + toStored(record.endTime())) / 2;
        }
        return midTimes;
    }

    long[][] bounds(List<VortexDataInterval> records) {
        long[][] bounds = new long[records.size()][2];
        for (int index = 0; index < records.size(); index++) {
            VortexDataInterval record = records.get(index);
            bounds[index][0] = toStored(record.startTime());
            bounds[index][1] = toStored(record.endTime());
        }
        return bounds;
    }

    /* Helpers */
    /**
     * A record as the destination will hold it. A time that falls between two ticks of the
     * destination's unit is pulled down to the tick before it, which is what writing it does anyway.
     */
    private static VortexDataInterval quantize(VortexDataInterval record, ChronoUnit unit) {
        if (!VortexDataInterval.isDefined(record)) {
            return null;
        }
        return VortexDataInterval.of(fromStored(toStored(record.startTime(), unit), unit),
                fromStored(toStored(record.endTime(), unit), unit));
    }

    private long toStored(ZonedDateTime time) {
        return toStored(time, unit);
    }

    private static long toStored(ZonedDateTime time, ChronoUnit unit) {
        return NetcdfTimeUtils.getNumIntervalsFromBaseTime(time, unit);
    }

    private static ZonedDateTime fromStored(long intervals, ChronoUnit unit) {
        return NetcdfTimeUtils.addToBaseTime(intervals, unit);
    }

    private static long[] readLongs(Variable variable) throws IOException, InvalidRangeException {
        Array array = variable.read();
        return (long[]) array.get1DJavaArray(DataType.LONG);
    }

    private static ChronoUnit readTimeUnit(String destination, Variable timeVariable) {
        String units = timeVariable.findAttributeString(CF.UNITS, "");
        try {
            return NetcdfTimeUtils.getDeclaredTimeUnit(units);
        } catch (IllegalArgumentException e) {
            throw DataWriteException.of(destination,
                    "its '" + CF.TIME + "' variable is not encoded in the minutes, hours or days of a CF grid file"
                            + " (found '" + units + "')", e);
        }
    }
}
