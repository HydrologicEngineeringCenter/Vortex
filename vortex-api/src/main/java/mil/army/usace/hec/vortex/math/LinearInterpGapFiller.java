package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LinearInterpGapFiller extends BatchGapFiller {

    private static final Logger LOGGER = Logger.getLogger(LinearInterpGapFiller.class.getName());

    LinearInterpGapFiller(Builder builder) {
        super(builder);
    }

    @Override
    public void run() {
        condenseVariables();

        for (String variable : variables) {
            try (DataReader reader = DataReader.builder()
                    .path(source)
                    .variable(variable)
                    .build()) {

                int dtoCount = reader.getDtoCount();

                ZonedDateTime[] times = new ZonedDateTime[dtoCount];
                byte[][] isNoData = new byte[dtoCount][];

                Set<Duration> intervals = new HashSet<>();

                Set<Integer> hasNoData = new LinkedHashSet<>();
                for (int i = 0; i < dtoCount; i++) {
                    VortexGrid vortexGrid = (VortexGrid) reader.getDto(i);

                    double noDataValue = vortexGrid.noDataValue();
                    float[] data = vortexGrid.data();
                    isNoData[i] = new byte[data.length];
                    for (int j = 0; j < data.length; j++) {
                        if (Double.compare(noDataValue, data[j]) == 0) {
                            isNoData[i][j] = 1;
                            hasNoData.add(i);
                        }
                    }

                    times[i] = vortexGrid.startTime();

                    intervals.add(vortexGrid.interval());
                    if (intervals.size() != 1)
                        throw new IllegalStateException("Data interval must be consistent");
                }

                for (int i = 0; i < dtoCount; i++) {
                    VortexGrid vortexGrid = (VortexGrid) reader.getDto(i);

                    List<VortexData> vortexGrids = new ArrayList<>();

                    if (hasNoData.contains(i)) {
                        float[] data = vortexGrid.data();
                        byte[] isNoDataI = isNoData[i];

                        for (int j = 0; j < isNoDataI.length; j++) {
                            if (isNoDataI[j] == 1) {
                                int k = i - 1;
                                boolean dataFound = false;
                                while (k >= 0) {
                                    if (isNoData[k][j] == 0) {
                                        dataFound = true;
                                        break;
                                    }
                                    k--;
                                }

                                if (!dataFound)
                                    continue;

                                dataFound = false;
                                int l = i + 1;
                                while (l < isNoDataI.length) {
                                    if (isNoData[l][j] == 0) {
                                        dataFound = true;
                                        break;
                                    }
                                    l++;
                                }

                                if (!dataFound)
                                    continue;

                                long t = times[i].toEpochSecond();

                                long t0 = times[k].toEpochSecond();
                                long t1 = times[l].toEpochSecond();

                                float d0 = ((VortexGrid) reader.getDto(k)).data()[j];
                                float d1 = ((VortexGrid) reader.getDto(l)).data()[j];

                                data[j] = interpolate(t, t0, d0, t1, d1);
                            }
                        }

                        VortexGrid filled = VortexGrid.toBuilder(vortexGrid)
                                .data(data)
                                .build();

                        vortexGrids.add(filled);
                    } else {
                        vortexGrids.add(vortexGrid);
                    }

                    DataWriter writer = DataWriter.builder()
                            .data(vortexGrids)
                            .destination(destination)
                            .options(writeOptions)
                            .build();

                    writer.write();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e, e::getMessage);
            }
        }
    }

    // Method to perform linear interpolation
    private static float interpolate(float x, float x1, float y1, float x2, float y2) {
        // Check if x1 and x2 are the same to avoid division by zero
        if (x1 == x2) {
            throw new IllegalArgumentException("x1 and x2 cannot be the same value.");
        }
        // Linear interpolation formula
        return y1 + ((x - x1) * (y2 - y1)) / (x2 - x1);
    }
}
