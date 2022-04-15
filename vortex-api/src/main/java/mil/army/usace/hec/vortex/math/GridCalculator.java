package mil.army.usace.hec.vortex.math;

public class GridCalculator {

    private final float[] data1;
    private final float[] data2;
    private final Operation operation;

    private GridCalculator(Builder builder){
        data1 = builder.data;
        data2 = builder.rasterData;
        operation = builder.operation;
    }

    public static class Builder {
        private float[] data;
        private float[] rasterData;
        private Operation operation;

        public Builder data(float[] data1){
            this.data = data1;
            return this;
        }

        public Builder rasterData(float[] rasterData){
            this.rasterData = rasterData;
            return this;
        }

        public Builder operation(Operation operation) {
            this.operation = operation;
            return this;
        }

        public GridCalculator build(){
            if (data == null)
                throw new IllegalStateException("Data 1 must be provided");
            if (rasterData == null)
                throw new IllegalStateException("Data 2 must be provided");
            if (data.length != rasterData.length)
                throw new IllegalStateException("Array lengths must be equal");
            if (operation == null)
                throw new IllegalStateException("Operation must be provided");
            return new GridCalculator(this);
        }
    }

    public static Builder builder(){return new Builder();}

    public float[] calculate(){

        int size = data1.length;
        float[] result = new float[size];

        switch (operation) {
            case MULTIPLY:
                for (int i = 0; i < size; i++) {
                    result[i] = data1[i] * data2[i];
                }
                break;
            case DIVIDE:
                for (int i = 0; i < size; i++) {
                    result[i] = data1[i] / data2[i];
                }
                break;
            case ADD:
                for (int i = 0; i < size; i++) {
                    result[i] = data1[i] + data2[i];
                }
                break;
            case SUBTRACT:
                for (int i = 0; i < size; i++) {
                    result[i] = data1[i] - data2[i];
                }
                break;
        }

        return result;
    }

}
