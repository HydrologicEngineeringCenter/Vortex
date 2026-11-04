package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import ucar.nc2.write.NetcdfFormatWriter;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

class NetcdfDataWriter extends DataWriter {
    /** Option that replaces the destination instead of adding this batch's time steps to it. */
    static final String OVERWRITE_OPTION = "isOverwrite";

    private final List<VortexGrid> vortexGridList;

    /* Constructor */
    NetcdfDataWriter(Builder builder) {
        super(builder);
        vortexGridList = data.stream()
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .toList();
    }

    /* Write */
    @Override
    public void write() {
        VortexGridCollection gridCollection = VortexGridCollection.of(vortexGridList);
        NetcdfTimeAxis timeAxis = NetcdfWriterPrep.prepareForWriting(destination.toString(), gridCollection,
                getTimeRecords(), getOverwritePreference(options));
        appendData(timeAxis);
    }

    /**
     * Writes this batch's grids to the records {@code timeAxis} says they belong to. Package visible
     * because {@link NetcdfBatchImporter} prepares a destination once, with every time step of the
     * import, and then writes the import through here one buffer at a time.
     */
    void appendData(NetcdfTimeAxis timeAxis) {
        NetcdfFormatWriter.Builder writerBuilder = NetcdfWriterPrep.appendingWriterBuilder(destination.toString());

        NetcdfGridWriter gridWriter = NetcdfGridWriter.create(vortexGridList);
        gridWriter.addListener(writerPropertyListener());
        gridWriter.appendData(writerBuilder, timeAxis, destination.toString());
    }

    /**
     * The time steps of this batch. Grids of different variables share a time step, so the steps repeat
     * here as often as there are variables; the destination holds one record for each.
     */
    private List<VortexDataInterval> getTimeRecords() {
        return vortexGridList.stream()
                .map(VortexDataInterval::of)
                .filter(VortexDataInterval::isDefined)
                .distinct()
                .toList();
    }

    /**
     * Whether the destination is replaced rather than extended. Absent means extend: replacing on every
     * write is what left an export with only its last batch of time steps, so a caller that wants it has
     * to ask for it by name. A batched export that asks for it asks for it on every batch, and so keeps
     * only the last one.
     */
    static boolean getOverwritePreference(Map<String, String> options) {
        return Boolean.parseBoolean(options.get(OVERWRITE_OPTION));
    }

    private PropertyChangeListener writerPropertyListener() {
        return evt -> {
            VortexProperty property = VortexProperty.parse(evt.getPropertyName());

            if (VortexProperty.ERROR == property) {
                String errorMessage = String.valueOf(evt.getNewValue());
                fireWriteError(errorMessage);
            }
        };
    }

}
