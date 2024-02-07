package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;

import java.util.Arrays;

public class Calculator {

    private final VortexGrid inputGrid;
    private final float multiplyValue;
    private final float divideValue;
    private final float addValue;
    private final float subtractValue;

    private Calculator(Builder builder){
        inputGrid = builder.input;
        multiplyValue = builder.multiplyValue;
        divideValue = builder.divideValue;
        addValue = builder.addValue;
        subtractValue = builder.subtractValue;
    }

    public static class Builder {
        private VortexGrid input;
        private float multiplyValue = Float.NaN;
        private float divideValue = Float.NaN;
        private float addValue = Float.NaN;
        private float subtractValue = Float.NaN;

        public Builder inputGrid(VortexGrid input){
            this.input = input;
            return this;
        }

        public Builder multiplyValue(float multiplyValue) {
            this.multiplyValue = multiplyValue;
            return this;
        }

        public Builder divideValue(float divideValue) {
            this.divideValue = divideValue;
            return this;
        }

        public Builder addValue(float addValue) {
            this.addValue = addValue;
            return this;
        }

        public Builder subtractValue(float subtractValue) {
            this.subtractValue = subtractValue;
            return this;
        }

        public Calculator build(){
            if (input == null)
                throw new IllegalStateException("Input grid must not be null");
            int count = 0;
            if (!Double.isNaN(multiplyValue))
                count++;
            if (!Double.isNaN(divideValue))
                count++;
            if (!Double.isNaN(addValue))
                count++;
            if(!Double.isNaN(subtractValue))
                count++;
            if (count != 1)
                throw new IllegalStateException("More than one operator initialized");
            return new Calculator(this);
        }
    }

    public static Builder builder(){return new Builder();}

    public VortexGrid calculate(){
        float[] data = inputGrid.data();
        int size = data.length;
        float[] resultantData = new float[size];

        float noDataValue = (float) inputGrid.noDataValue();

        Arrays.fill(resultantData, noDataValue);

        if (!Double.isNaN(multiplyValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] != noDataValue ? data[i] * multiplyValue : noDataValue;
            }
        }

        if (!Double.isNaN(divideValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] != noDataValue ? data[i] / divideValue : noDataValue;
            }
        }

        if (!Double.isNaN(addValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] != noDataValue ? data[i] + addValue : noDataValue;
            }
        }

        if (!Double.isNaN(subtractValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] != noDataValue ? data[i] - subtractValue : noDataValue;
            }
        }

        return VortexGrid.toBuilder(inputGrid)
                .data(resultantData)
                .build();
    }

}
