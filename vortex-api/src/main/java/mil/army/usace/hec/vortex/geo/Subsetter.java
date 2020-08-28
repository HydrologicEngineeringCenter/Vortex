package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.locationtech.jts.geom.Envelope;

public class Subsetter {
    private final VortexGrid grid;
    private final Envelope envelope;

    private Subsetter(Builder builder) {
        grid = builder.grid;
        envelope = builder.envelope;
    }

    public static class Builder {
        private VortexGrid grid;
        private Envelope envelope;

        public Builder setGrid(VortexGrid grid) {
            this.grid = grid;
            return this;
        }

        public Builder setEnvelope(Envelope envelope) {
            this.envelope = envelope;
            return this;
        }

        public Subsetter build() {
            if(grid == null){
                throw new IllegalArgumentException("Input grid must be provided");
            }
            if (envelope == null) {
                throw new IllegalArgumentException("Subsetting envelope must be provided");
            }
            return new Subsetter(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public VortexGrid subset() {
        double originX = grid.originX();
        double originY = grid.originY();
        double dx = grid.dx();
        double dy = grid.dy();
        int nx = grid.nx();
        int ny = grid.ny();

        int offsetX = (int) Math.floor((envelope.getMinX() - originX) / dx);
        int offsetY = (int) Math.floor((envelope.getMaxY() - originY) / dy);
        int subsetNx = Math.min((int) Math.ceil((envelope.getMaxX() - originX) / dx) - offsetX, nx - offsetX);
        int subsetNy = Math.min((int) Math.ceil((envelope.getMinY() - originY) / dy) - offsetY, ny - offsetY);
        double subsetOriginX = originX + offsetX * dx;
        double subsetOriginY = originY + offsetY * dy;

        Dataset dataset = RasterUtils.getDatasetFromVortexGrid(grid);
        Band band = dataset.GetRasterBand(1);
        float[] subsetData = new float[subsetNx * subsetNy];
        band.ReadRaster(offsetX, offsetY, subsetNx, subsetNy, subsetData);
        band.delete();

        return  VortexGrid.builder()
                .dx(dx)
                .dy(dy)
                .nx(subsetNx)
                .ny(subsetNy)
                .originX(subsetOriginX)
                .originY(subsetOriginY)
                .wkt(grid.wkt())
                .data(subsetData)
                .units(grid.units())
                .fileName(grid.fileName())
                .shortName(grid.shortName())
                .fullName(grid.fullName())
                .description(grid.description())
                .startTime(grid.startTime())
                .endTime(grid.endTime())
                .interval(grid.interval())
                .build();
    }
}
