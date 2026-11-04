package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class NetcdfGridWriter {
    private static final Logger logger = Logger.getLogger(NetcdfGridWriter.class.getName());
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private final Map<String, VortexGridCollection> gridCollectionMap;

    private NetcdfGridWriter(List<VortexGrid> vortexGridList) {
        gridCollectionMap = initGridCollectionMap(vortexGridList);
    }

    static NetcdfGridWriter create(List<VortexGrid> vortexGridList) {
        return new NetcdfGridWriter(vortexGridList);
    }

    private Map<String, VortexGridCollection> initGridCollectionMap(List<VortexGrid> vortexGridList) {
        Map<String, VortexGridCollection> map = vortexGridList.stream()
                .collect(Collectors.groupingBy(
                        VortexGrid::shortName,
                        Collectors.collectingAndThen(Collectors.toList(), VortexGridCollection::of))
                );
        boolean isValidMap = verifyGridCollectionMap(map);
        return isValidMap ? map : Collections.emptyMap();
    }

    private boolean verifyGridCollectionMap(Map<String, VortexGridCollection> map) {
        boolean projectionMatched = isUnique(VortexGridCollection::getProjection, map);
        // No need to check lat & lon since they are generated from (x & y & projection)
        boolean yMatched = isUnique(VortexGridCollection::getYCoordinates, map);
        boolean xMatched = isUnique(VortexGridCollection::getXCoordinates, map);
        return projectionMatched && yMatched && xMatched;
    }

    private boolean isUnique(Function<VortexGridCollection, ?> propertyGetter, Map<String, VortexGridCollection> map) {
        boolean isUnique = map.values().stream()
                .map(propertyGetter)
                .map(o -> (o instanceof double[] || o instanceof float[]) ? Arrays.deepToString(new Object[] {o}) : o)
                .distinct()
                .count() == 1;
        if (!isUnique) {
            logger.severe("Data is not the same for all grids");
        }
        return isUnique;
    }

    /* Append Data */
    void appendData(NetcdfFormatWriter.Builder writerBuilder, NetcdfTimeAxis timeAxis, String destination) {
        try (NetcdfFormatWriter writer = writerBuilder.build()) {
            if (writer.findVariable(CF.TIME) == null) {
                logger.warning(() -> "No grids were written to " + destination + ": a grid with no time steps has no"
                        + " record to write to, and the NetCDF writer stores one record per time step.");
                return;
            }

            writeVariableGrids(writer, timeAxis, destination);
        } catch (IOException e) {
            throw DataWriteException.of(destination, "its data could not be written (" + e.getMessage() + ")", e);
        }
    }

    private void writeVariableGrids(NetcdfFormatWriter writer, NetcdfTimeAxis timeAxis, String destination) {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());

        for (VortexGridCollection collection : gridCollectionMap.values()) {
            String variableName = collection.getVariableName();
            Variable variable = writer.findVariable(variableName);
            if (variable == null) {
                failures.add("the destination holds no '" + variableName + "' variable");
                continue;
            }

            collection.getCollectionDataStream().forEach(entry -> {
                VortexGrid grid = entry.getValue();
                int record = timeAxis.indexOf(VortexDataInterval.of(grid));
                if (record < 0) {
                    failures.add("'" + variableName + "' from " + grid.startTime() + " to " + grid.endTime()
                            + " has no record in the destination's time axis");
                    return;
                }

                try {
                    int[] origin = {record, 0, 0};
                    writer.write(variable, origin, Array.makeFromJavaArray(grid.data3D()));
                } catch (IOException | InvalidRangeException e) {
                    failures.add("'" + variableName + "' could not be written at record " + record + " (" + e.getMessage() + ")");
                }
            });
        }

        if (!failures.isEmpty()) {
            reportFailures(destination, failures);
        }
    }

    /**
     * Reports through both channels a caller has: the property change event a GUI is listening to, and
     * an exception the caller of {@link DataWriter#write()} gets whether or not it is listening. A
     * failed write that returns normally is indistinguishable from one that wrote the data.
     */
    private void reportFailures(String destination, List<String> failures) {
        String reason = failures.stream().distinct()
                .collect(Collectors.joining("\n* ", "one or more grids were not written:\n* ", ""));
        support.firePropertyChange(VortexProperty.ERROR.toString(), null, reason);
        throw DataWriteException.of(destination, reason);
    }

    /* Property Change */
    public void addListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}
