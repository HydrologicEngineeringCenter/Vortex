package mil.army.usace.hec.vortex;

import org.gdal.gdal.gdal;
import org.gdal.osr.osr;

public enum GdalRegister {
    INSTANCE;

    /*
     * jpackage-built Windows launchers cannot set OS environment variables the way
     * Launch4j and the old .bat scripts did, so GDAL_DATA/PROJ_LIB are instead
     * threaded through as JVM system properties baked into each launcher's
     * --java-options (see vortex-ui/build.gradle.kts).
     *
     * PATH itself (needed so gdal302.dll's own dependency chain resolves; a
     * same-folder java.library.path is not sufficient -- see VortexUi's
     * re-exec-with-corrected-PATH logic) cannot be fixed at this layer, since
     * by the time GdalRegister runs, the process's native loader has already
     * made its decision for anything loaded so far.
     */
    private static final String GDAL_DATA_PROPERTY = "vortex.gdal.data";
    private static final String PROJ_LIB_PROPERTY = "vortex.proj.lib";

    GdalRegister(){
        try {
            configureGdalPaths();
            gdal.AllRegister();
        } catch (UnsatisfiedLinkError e){
            System.out.println(e);
        }
    }

    public static GdalRegister getInstance(){
        return INSTANCE;
    }

    private static void configureGdalPaths() {
        String gdalData = System.getProperty(GDAL_DATA_PROPERTY);
        if (gdalData != null && !gdalData.isEmpty()) {
            gdal.SetConfigOption("GDAL_DATA", gdalData);
        }

        // PROJ reads PROJ_LIB only from the OS environment; GDAL 3.2 does not
        // forward a PROJ_LIB config option to it, so SetConfigOption would leave
        // PROJ unable to find proj.db. Hand PROJ the search path directly.
        String projLib = System.getProperty(PROJ_LIB_PROPERTY);
        if (projLib != null && !projLib.isEmpty()) {
            osr.SetPROJSearchPath(projLib);
        }
    }
}
