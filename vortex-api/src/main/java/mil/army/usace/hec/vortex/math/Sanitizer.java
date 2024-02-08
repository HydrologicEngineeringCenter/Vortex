package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;

public class Sanitizer {

    private final VortexGrid input;
    private final double minimumThreshold;
    private final double maximumThreshold;
    private final float minimumReplacementValue;
    private final float maximumReplacementValue;

    private Sanitizer(Builder builder){
        input = builder.input;
        minimumThreshold = builder.minimumThreshold;
        maximumThreshold = builder.maximumThreshold;
        minimumReplacementValue = builder.minimumReplacementValue;
        maximumReplacementValue = builder.maximumReplacementValue;
    }

    public static class Builder {
        private VortexGrid input;
        private double minimumThreshold = -Double.MAX_VALUE;
        private double maximumThreshold = Double.MAX_VALUE;
        private float minimumReplacementValue = Float.NaN;
        private float maximumReplacementValue = Float.NaN;

        public Builder inputGrid(VortexGrid input){
            this.input = input;
            return this;
        }

        public Builder minimumThreshold(double minimumThreshold) {
            this.minimumThreshold = minimumThreshold;
            return this;
        }

        public Builder maximumThreshold(double maximumThreshold) {
            this.maximumThreshold = maximumThreshold;
            return this;
        }

        public Builder minimumReplacementValue(float minimumReplacementValue) {
            this.minimumReplacementValue = minimumReplacementValue;
            return this;
        }

        public Builder maximumReplacementValue(float maximumReplacementValue) {
            this.maximumReplacementValue = maximumReplacementValue;
            return this;
        }

        public Sanitizer build(){
            return new Sanitizer(this);
        }
    }

    public static Builder builder(){return new Builder();}

    public VortexGrid sanitize(){
        float[] data = input.data();
        int size = data.length;
        float[] sanitizedData = new float[size];

        for (int i = 0; i < size; i++) {
            if (data[i] <= minimumThreshold) {
                sanitizedData[i] = minimumReplacementValue;
            } else if (data[i] >= maximumThreshold) {
                sanitizedData[i] = maximumReplacementValue;
            } else {
                sanitizedData[i] = data[i];
            }
        }

        return VortexGrid.toBuilder(input)
                .data(sanitizedData)
                .build();
    }

}
