package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScalerTest {

    @Test
    void GridIsScaledByTwo(){
        VortexGrid grid = mock(VortexGrid.class);
        when(grid.data()).thenReturn(new float[]{0, (float) 0.5, 1, 2});

        Scaler scaler = Scaler.builder()
                .grid(grid)
                .scaleFactor(2)
                .build();

        VortexGrid scaled = scaler.scale();

        float[] expected = new float[]{0, 1, 2, 4};
        for(int i = 0; i < expected.length; i++){
            Assertions.assertEquals(expected[i], scaled.data()[i]);
        }
    }

    @Test
    void GridIsScaledByOneHalf(){
        VortexGrid grid = mock(VortexGrid.class);
        when(grid.data()).thenReturn(new float[]{0, (float) 0.5, 1, 2});

        Scaler scaler = Scaler.builder()
                .grid(grid)
                .scaleFactor(0.5)
                .build();

        VortexGrid scaled = scaler.scale();

        float[] expected = new float[]{0, (float) 0.25, (float) 0.5, 1};
        for(int i = 0; i < expected.length; i++){
            Assertions.assertEquals(expected[i], scaled.data()[i]);
        }
    }

    @Test
    void NoFactorSupplied(){
        VortexGrid grid = mock(VortexGrid.class);
        when(grid.data()).thenReturn(new float[]{0, (float) 0.5, 1, 2});

        Scaler scaler = Scaler.builder()
                .grid(grid)
                .build();

        VortexGrid scaled = scaler.scale();

        float[] expected = new float[]{0, (float) 0.5, 1, 2};
        for(int i = 0; i < expected.length; i++){
            Assertions.assertEquals(expected[i], scaled.data()[i]);
        }
    }


}