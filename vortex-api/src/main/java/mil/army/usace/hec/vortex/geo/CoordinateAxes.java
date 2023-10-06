package mil.army.usace.hec.vortex.geo;

import ucar.nc2.dataset.CoordinateAxis;

public record CoordinateAxes(CoordinateAxis xAxis, CoordinateAxis yAxis) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoordinateAxes that = (CoordinateAxes) o;
        return xAxis.equals(that.xAxis) && yAxis.equals(that.yAxis);
    }

}
