package mil.army.usace.hec.vortex;

import org.gdal.gdal.gdal;

public enum GdalRegister {
    INSTANCE;

    GdalRegister(){
        try {
            gdal.AllRegister();
        } catch (UnsatisfiedLinkError e){
            System.out.println(e);
        }
    }

    public static GdalRegister getInstance(){
        return INSTANCE;
    }
}
