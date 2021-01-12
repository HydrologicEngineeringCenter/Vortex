package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @Test
    void multiply() {
        float[] data = new float[]{0, -2, 2, 0.25f};

        VortexGrid inputGrid = VortexGrid.builder()
                .dx(1).dy(-1)
                .nx(2).ny(2)
                .originX(0)
                .originY(0)
                .wkt("")
                .data(data)
                .units("")
                .fileName("")
                .shortName("")
                .fullName("")
                .description("")
                .startTime(ZonedDateTime.now())
                .endTime(ZonedDateTime.now())
                .interval(Duration.ZERO)
                .build();

        Calculator calculator = Calculator.builder()
                .inputGrid(inputGrid)
                .multiplyValue(2)
                .build();

        VortexGrid outputGrid = calculator.calculate();

        float[] expectedData = new float[]{0, -4, 4, 0.5f};

        assertArrayEquals(expectedData, outputGrid.data());
    }

    @Test
    void divide() {
        float[] data = new float[]{0, -2, 2, 0.25f};

        VortexGrid inputGrid = VortexGrid.builder()
                .dx(1).dy(-1)
                .nx(2).ny(2)
                .originX(0)
                .originY(0)
                .wkt("")
                .data(data)
                .units("")
                .fileName("")
                .shortName("")
                .fullName("")
                .description("")
                .startTime(ZonedDateTime.now())
                .endTime(ZonedDateTime.now())
                .interval(Duration.ZERO)
                .build();

        Calculator calculator = Calculator.builder()
                .inputGrid(inputGrid)
                .divideValue(2)
                .build();

        VortexGrid outputGrid = calculator.calculate();

        float[] expectedData = new float[]{0, -1, 1, 0.125f};

        assertArrayEquals(expectedData, outputGrid.data());
    }
    @Test
    void add() {
        float[] data = new float[]{0, -2, 2, 0.25f};

        VortexGrid inputGrid = VortexGrid.builder()
                .dx(1).dy(-1)
                .nx(2).ny(2)
                .originX(0)
                .originY(0)
                .wkt("")
                .data(data)
                .units("")
                .fileName("")
                .shortName("")
                .fullName("")
                .description("")
                .startTime(ZonedDateTime.now())
                .endTime(ZonedDateTime.now())
                .interval(Duration.ZERO)
                .build();

        Calculator calculator = Calculator.builder()
                .inputGrid(inputGrid)
                .addValue(2)
                .build();

        VortexGrid outputGrid = calculator.calculate();

        float[] expectedData = new float[]{2, 0, 4, 2.25f};

        assertArrayEquals(expectedData, outputGrid.data());
    }

    @Test
    void subtract() {
        float[] data = new float[]{0, -2, 2, 0.25f};

        VortexGrid inputGrid = VortexGrid.builder()
                .dx(1).dy(-1)
                .nx(2).ny(2)
                .originX(0)
                .originY(0)
                .wkt("")
                .data(data)
                .units("")
                .fileName("")
                .shortName("")
                .fullName("")
                .description("")
                .startTime(ZonedDateTime.now())
                .endTime(ZonedDateTime.now())
                .interval(Duration.ZERO)
                .build();

        Calculator calculator = Calculator.builder()
                .inputGrid(inputGrid)
                .subtractValue(2)
                .build();

        VortexGrid outputGrid = calculator.calculate();

        float[] expectedData = new float[]{-2, -4, 0, -1.75f};

        assertArrayEquals(expectedData, outputGrid.data());
    }
}