package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeographicProcessor {
    private final boolean isEmpty;
    private final Rectangle2D env;
    private final String envWkt;
    private final String destWkt;
    private final double cellSize;
    private final String method;

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
            env = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
            envWkt = geoOptions.get("envWkt");
        } else {
            env = null;
            envWkt = null;
        }

        method = geoOptions.getOrDefault("resamplingMethod", "near");

        String wktValue = geoOptions.get("targetWkt");
        String epsgValue = geoOptions.get("targetEpsg");

        if (epsgValue != null) {
            int epsg = Integer.parseInt(epsgValue);
            destWkt = WktFactory.fromEpsg(epsg);
        } else {
            destWkt = wktValue;
        }

        if (geoOptions.containsKey("targetCellSize")) {
            cellSize = Double.parseDouble(geoOptions.get("targetCellSize"));
        } else {
            cellSize = Double.NaN;
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
                .method(method)
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