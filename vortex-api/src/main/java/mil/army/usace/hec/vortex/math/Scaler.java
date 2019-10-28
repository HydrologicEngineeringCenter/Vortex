package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;

public class Scaler {

    private VortexGrid grid;
    private double factor;

    private Scaler(ScalerBuilder builder){
        this.grid = builder.grid;
        this.factor = builder.factor;
    }

    public static class ScalerBuilder{
        private VortexGrid grid;
        private Double factor;

        public ScalerBuilder grid(VortexGrid grid){
            this.grid = grid;
            return this;
        }

        public ScalerBuilder scaleFactor(double factor){
            this.factor = factor;
            return this;
        }

        public Scaler build(){
            if (factor == null || factor.isNaN()){
                factor = 1.0;
            }
            return new Scaler(this);
        }
    }

    public static ScalerBuilder builder(){return new ScalerBuilder();}

    public VortexGrid scale(){
        float[] data = grid.data();
        int count = data.length;
        float[] scaled = new float[count];
        for (int i = 0; i < count; i++){
            scaled[i] = (float) (data[i] * factor);
        }
        return VortexGrid.builder()
                .dx(grid.dx()).dy(grid.dy()).nx(grid.nx()).ny(grid.ny())
                .originX(grid.originX()).originY(grid.originY())
                .wkt(grid.wkt()).data(scaled).units(grid.units())
                .fileName(grid.fileName()).shortName(grid.shortName())
                .fullName(grid.fullName()).description(grid.description())
                .startTime(grid.startTime()).endTime(grid.endTime())
                .interval(grid.interval()).build();
    }
}
