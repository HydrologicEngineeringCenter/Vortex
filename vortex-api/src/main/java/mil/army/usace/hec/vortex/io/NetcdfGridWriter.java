package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.*;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NetcdfGridWriter {
    private static final Logger logger = Logger.getLogger(NetcdfGridWriter.class.getName());
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    public static final int BOUNDS_LEN = 2;

    private final Map<String, VortexGridCollection> gridCollectionMap;
    private final VortexGridCollection defaultCollection;

    public NetcdfGridWriter(List<VortexGrid> vortexGridList) {
        gridCollectionMap = initGridCollectionMap(vortexGridList);
        defaultCollection = gridCollectionMap.values().stream()
                .findAny()
                .orElse(new VortexGridCollection(Collections.emptyList()));
    }

    private Map<String, VortexGridCollection> initGridCollectionMap(List<VortexGrid> vortexGridList) {
        Map<String, VortexGridCollection> map = vortexGridList.stream()
                .collect(Collectors.groupingBy(
                        VortexGrid::shortName,
                        Collectors.collectingAndThen(Collectors.toList(), VortexGridCollection::new))
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

    private void writeVariableGrids(NetcdfFormatWriter writer) {
        AtomicBoolean hasErrors = new AtomicBoolean(false);

        for (VortexGridCollection collection : gridCollectionMap.values()) {
            VortexVariable meteorologicalVariable = getVortexVariable(collection);
            Variable variable = writer.findVariable(meteorologicalVariable.getShortName());
            if (variable == null) {
                logger.severe("Failed to locate variable: " + meteorologicalVariable.getShortName());
                hasErrors.set(true);
                continue;
            }

            collection.getCollectionDataStream().forEach(entry -> {
                try {
                    VortexGrid grid = entry.getValue();
                    int index = NetcdfWriterPrep.getTimeRecordIndex(writer.getOutputFile().getLocation(), VortexTimeRecord.of(grid));
                    int[] origin = {index, 0, 0};
                    writer.write(variable, origin, Array.makeFromJavaArray(grid.data3D()));
                } catch (IOException | InvalidRangeException e) {
                    logger.warning(e.getMessage());
                    hasErrors.set(true);
                }
            });
        }

        if (hasErrors.get()) {
//            boolean isAppend = startIndex > 0;
            String overwriteErrorMessage = "Failed to overwrite file.";
            String appendErrorMessage = "Some reasons may be:\n* Attempted to append to non-existing variable\n* Attempted to append data with different projection\n* Attempted to append data with different location";
//            String message = isAppend ? appendErrorMessage : overwriteErrorMessage;
            support.firePropertyChange(VortexProperty.ERROR, null, appendErrorMessage);
        }
    }

    /* Helpers */
    private static VortexVariable getVortexVariable(VortexGridCollection collection) {
        VortexVariable name = VortexVariable.fromName(collection.getShortName());
        return name.equals(VortexVariable.UNDEFINED) ? VortexVariable.fromName(collection.getDescription()) : name;
    }

    /* Property Change */
    public void addListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }

    /* Append Data */
    public void appendData(NetcdfFormatWriter.Builder writerBuilder) {
        try (NetcdfFormatWriter writer = writerBuilder.build()) {
            Variable timeVar = writer.findVariable(CF.TIME);
            if (timeVar == null) {
                logger.severe("Time variable not found");
                return;
            }

//            int startIndex = (int) timeVar.getSize();
//            writeVariableGrids(writer, startIndex);
            writeVariableGrids(writer);
//            writer.write(timeVar, new int[] {startIndex}, Array.makeFromJavaArray(defaultCollection.getTimeData()));

//            if (defaultCollection.hasTimeBounds()) {
//                writer.write("time_bnds", new int[] {startIndex, 0}, Array.makeFromJavaArray(defaultCollection.getTimeBoundsArray()));
//            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }
}
