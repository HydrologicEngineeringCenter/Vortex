package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;

public class Sanatizer {

    private VortexGrid input;

    private Sanatizer(SanatizerBuilder builder){
        this.input = builder.input;
    }

    public static class SanatizerBuilder{
        private VortexGrid input;

        public SanatizerBuilder inputGrid(VortexGrid input){
            this.input = input;
            return this;
        }

        public Sanatizer build(){
            return new Sanatizer(this);
        }
    }

    public static SanatizerBuilder builder(){return new SanatizerBuilder();}

    public VortexGrid sanatize(){
        float[] data = input.data();
        int size = data.length;
        float[] sanatizedData = new float[size];
        String desc = input.description().toLowerCase();
        if (desc.contains("precipitation")
                || desc.contains("precip")
                || desc.contains("precip") && desc.contains("rate")
                || desc.contains("qpe01h")
                || ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation"))
                || ((desc.contains("snow")) && (desc.contains("water")) && (desc.contains("equivalent")))
                || desc.contains("albedo")) {
            for(int i = 0; i < data.length; i++){
                if (data[i] < 0){
                    sanatizedData[i] = Float.NaN;
                } else {
                    sanatizedData[i] = data[i];
                }
            }
        } else {
            for(int i = 0; i < data.length; i++){
                if (data[i] < -1E20){
                    sanatizedData[i] = Float.NaN;
                } else {
                    sanatizedData[i] = data[i];
                }
            }
        }

        return VortexGrid.builder()
                .dx(input.dx()).dy(input.dy())
                .nx(input.nx()).ny(input.ny())
                .originX(input.originX())
                .originY(input.originY())
                .wkt(input.wkt())
                .data(sanatizedData)
                .units(input.units())
                .fileName(input.fileName())
                .shortName(input.shortName())
                .fullName(input.fullName())
                .description(input.description())
                .startTime(input.startTime())
                .endTime(input.endTime())
                .interval(input.interval())
                .build();
    }

}
