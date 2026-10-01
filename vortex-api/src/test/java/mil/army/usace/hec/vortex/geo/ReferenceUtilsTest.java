package mil.army.usace.hec.vortex.geo;

import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceUtilsTest {

    @ParameterizedTest
    @ValueSource(ints = {4267, 4326, 32610})
    void ValidatesGeographicAndProjectedCoordinateSystems(int epsg) {
        assertTrue(ReferenceUtils.isValidProjection(WktFactory.fromEpsg(epsg)));
    }

    @Test
    void ValidatesEsriProjectionWkt() {
        assertTrue(ReferenceUtils.isValidProjection("GEOGCS[\"GCS_North_American_1927\","
                + "DATUM[\"D_North_American_1927\",SPHEROID[\"Clarke_1866\",6378206.4,294.978698213898]],"
                + "PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.0174532925199433]]"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t", "not a projection", "GEOGCS[\"Incomplete\"]"})
    void RejectsMissingAndInvalidProjectionDefinitions(String wkt) {
        assertFalse(ReferenceUtils.isValidProjection(wkt));
    }

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

    @Test
    void UlyDirectionReturnsPositiveForUtmSouth(){
        String wkt = WktFactory.create("UTM36S");
        double llx = 302000.0;
        double lly = 7080000.0;
        int direction = ReferenceUtils.getUlyDirection(wkt, llx, lly);
        assertEquals(1, direction);
    }

    @Test
    void UlyDirectionReturnsPositiveForShg(){
        String wkt = WktFactory.getShg();
        double x = -1680000;
        double lly = 102000;
        int direction = ReferenceUtils.getUlyDirection(wkt, x, lly);
        assertEquals(1, direction);
    }

}
