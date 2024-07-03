package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.locationtech.jts.geom.Envelope;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tech.units.indriya.unit.Units.METRE;

public class GeographicProcessor {
    private final boolean isEmpty;
    private final Envelope env;
    private final String envWkt;
    private final String destWkt;
    private final Quantity<Length> cellSize;
    private final ResamplingMethod resamplingMethod;

    public GeographicProcessor(Map<String, String> geoOptions) {
        isEmpty = geoOptions.isEmpty();

        if (geoOptions.containsKey("pathToShp") && new File(geoOptions.get("pathToShp")).exists()) {
            env = VectorUtils.getEnvelope(Paths.get(geoOptions.get("pathToShp")));
            envWkt = VectorUtils.getWkt(Paths.get(geoOptions.get("pathToShp")));
        } else if (geoOptions.containsKey("minX") && geoOptions.containsKey("maxX")
                && geoOptions.containsKey("minY") && geoOptions.containsKey("maxY")
                && geoOptions.containsKey("envWkt")) {
            double minX = Double.parseDouble(geoOptions.get("minX"));
            double maxX = Double.parseDouble(geoOptions.get("maxX"));
            double minY = Double.parseDouble(geoOptions.get("minY"));
            double maxY = Double.parseDouble(geoOptions.get("maxY"));
            env =  new Envelope(minX, maxX, minY, maxY);
            envWkt = geoOptions.get("envWkt");
        } else {
            env = null;
            envWkt = null;
        }

        String methodString = geoOptions.getOrDefault("resamplingMethod", "near");
        resamplingMethod = ResamplingMethod.fromString(methodString);

        String wktValue = geoOptions.get("targetWkt");
        String epsgValue = geoOptions.get("targetEpsg");

        if (epsgValue != null) {
            int epsg = Integer.parseInt(epsgValue);
            destWkt = WktFactory.fromEpsg(epsg);
        } else {
            destWkt = wktValue;
        }

        if (geoOptions.containsKey("targetCellSize")) {
            String cellSizeString = geoOptions.get("targetCellSize");
            double cellSizeValue = Double.parseDouble(cellSizeString);

            // Cell size units defaults to meters
            String unitsString = geoOptions.get("targetCellSizeUnits");
            Unit<Length> cellSizeUnits = unitsString != null ? CellSizeUnits.of(unitsString).getUnits() : METRE;

            cellSize = Quantities.getQuantity(cellSizeValue, cellSizeUnits);
        } else {
            cellSize = null;
        }
    }

    public VortexGrid process(VortexGrid input) {
        if (isEmpty)
            return input;

        return Resampler.builder()
                .grid(input)
                .envelope(env)
                .envelopeWkt(envWkt)
                .targetWkt(destWkt)
                .cellSize(cellSize)
                .method(resamplingMethod)
                .build()
                .resample();
    }

    public List<VortexGrid> process(List<VortexGrid> data) {
        List<VortexGrid> output = new ArrayList<>();
        for (VortexGrid grid : data) {
            VortexGrid processed = process(grid);
            output.add(processed);
        }
        return output;
    }
}
