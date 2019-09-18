package mil.army.usace.hec.vortex.geo;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TravelLengthGridCellsReader {

    private Path pathToSource;

    private TravelLengthGridCellsReader(TravelLengthGridCellsReaderBuilder builder){
        this.pathToSource = builder.pathToSource;
    }

    public static class TravelLengthGridCellsReaderBuilder {
        private Path pathToSource;

        public TravelLengthGridCellsReaderBuilder pathToSource(String pathToSource){
            this.pathToSource = Paths.get(pathToSource);
            return this;
        }

        public TravelLengthGridCellsReader build(){
            if (pathToSource == null){
                throw new IllegalArgumentException("Path to source is required");
            }
            return new TravelLengthGridCellsReader(this);
        }
    }

    public static TravelLengthGridCellsReaderBuilder builder() {
        return new TravelLengthGridCellsReaderBuilder();
    }

    public List<TravelLengthGridCell> read(){
        ArrayList<TravelLengthGridCell> gridCells = new ArrayList<>();
        try (Scanner scanner = new Scanner(pathToSource.toFile())) {
            AtomicBoolean isSubbasinFound = new AtomicBoolean();
            AtomicReference<String> subbasinName = new AtomicReference<>();
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("Subbasin:")) {
                    subbasinName.set(line.split(":")[1].trim());
                    isSubbasinFound.set(true);
                }

                if (line.startsWith("End:")) {
                    isSubbasinFound.set(false);
                }

                if (isSubbasinFound.get() && line.trim().startsWith("GridCell:")) {
                    String[] split = line.trim().split("\\s+");
                    if (split.length != 5) {
                        System.out.println("Error: invalid grid cell definition " + line.trim());
                    }
                    TravelLengthGridCell gridCell = TravelLengthGridCell.builder()
                            .subbasin(subbasinName.get())
                            .indexI(Integer.parseInt(split[1]))
                            .indexJ(Integer.parseInt(split[2]))
                            .travelLength(Double.parseDouble(split[3]))
                            .area(Double.parseDouble(split[4]))
                            .build();

                    gridCells.add(gridCell);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return gridCells;
    }

    public boolean validate(){
        AtomicBoolean isValid = new AtomicBoolean(true);
        try (Scanner scanner = new Scanner(pathToSource.toFile())) {
            AtomicBoolean isSubbasinFound = new AtomicBoolean();
            AtomicReference<String> subbasinName = new AtomicReference<>();
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("Subbasin:")) {
                    subbasinName.set(line.split(":")[1].trim());
                    isSubbasinFound.set(true);
                }

                if (line.startsWith("End:")) {
                    isSubbasinFound.set(false);
                }

                if (isSubbasinFound.get() && line.trim().startsWith("GridCell:")) {
                    String[] split = line.trim().split("\\s+");
                    if (split.length != 5) {
                        System.out.println("Error: invalid grid cell definition: " + line.trim());
                        isValid.set(false);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return isValid.get();
    }
}
