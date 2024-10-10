package mil.army.usace.hec.vortex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VortexGridTest {
    @Test
    void getValue() {
        float[] data = new float[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) i * 5;
        }

        VortexGrid grid = VortexGrid.builder().data(data).build();
        assertEquals(0, grid.getValue(0));
        assertEquals(5, grid.getValue(1));
        assertEquals(10, grid.getValue(2));
        assertEquals(15, grid.getValue(3));
        assertEquals(20, grid.getValue(4));
        assertEquals(25, grid.getValue(5));
        assertEquals(30, grid.getValue(6));
        assertEquals(35, grid.getValue(7));
        assertEquals(40, grid.getValue(8));
        assertEquals(45, grid.getValue(9));
    }
}