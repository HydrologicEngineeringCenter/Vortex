package mil.army.usace.hec.vortex.geo;

import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceUtilsTest {

    @Test
    void IsShgReturnsTrueForShgWithProjectionUnitsM(){
        AlbersInfo info = mock(AlbersInfo.class);
        when(info.getGridType()).thenReturn(420);
        when(info.getFirstStandardParallel()).thenReturn((float) 29.5);
        when(info.getSecondStandardParallel()).thenReturn((float) 45.5);
        when(info.getLatitudeOfProjectionOrigin()).thenReturn((float) 23.0);
        when(info.getCentralMeridian()).thenReturn((float) -96.0);
        when(info.getFalseEasting()).thenReturn((float) 0);
        when(info.getFalseNorthing()).thenReturn((float) 0);
        when(info.getProjectionDatum()).thenReturn(GridInfo.getNad83());
        when(info.getProjectionUnits()).thenReturn("m");
        assertTrue(ReferenceUtils.isShg(info));
    }

    @Test
    void IsShgReturnsTrueForShgWithProjectionUnitsMeter(){
        AlbersInfo info = mock(AlbersInfo.class);
        when(info.getGridType()).thenReturn(420);
        when(info.getFirstStandardParallel()).thenReturn((float) 29.5);
        when(info.getSecondStandardParallel()).thenReturn((float) 45.5);
        when(info.getLatitudeOfProjectionOrigin()).thenReturn((float) 23.0);
        when(info.getCentralMeridian()).thenReturn((float) -96.0);
        when(info.getFalseEasting()).thenReturn((float) 0);
        when(info.getFalseNorthing()).thenReturn((float) 0);
        when(info.getProjectionDatum()).thenReturn(GridInfo.getNad83());
        when(info.getProjectionUnits()).thenReturn("meter");
        assertTrue(ReferenceUtils.isShg(info));
    }

    @Test
    void IsShgReturnsFalseForShgWithProjectionUnitsFeet(){
        AlbersInfo info = mock(AlbersInfo.class);
        when(info.getGridType()).thenReturn(420);
        when(info.getFirstStandardParallel()).thenReturn((float) 29.5);
        when(info.getSecondStandardParallel()).thenReturn((float) 45.5);
        when(info.getLatitudeOfProjectionOrigin()).thenReturn((float) 23.0);
        when(info.getCentralMeridian()).thenReturn((float) -96.0);
        when(info.getFalseEasting()).thenReturn((float) 0);
        when(info.getFalseNorthing()).thenReturn((float) 0);
        when(info.getProjectionDatum()).thenReturn(GridInfo.getNad83());
        when(info.getProjectionUnits()).thenReturn("ft");
        assertFalse(ReferenceUtils.isShg(info));
    }

}