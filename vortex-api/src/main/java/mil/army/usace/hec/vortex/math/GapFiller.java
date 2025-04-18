package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;

interface GapFiller {
    VortexGrid fill(VortexGrid grid);

    static GapFiller of(GapFillMethod method) {
        if (method == GapFillMethod.FOCAL_MEAN)
            return FocalMeanGapFiller.newInstance();

        throw new IllegalArgumentException("Gap fill method \"" + method + "\" not supported");
    }
}
