package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.util.DssUtil;

import java.util.*;

final class DssWriteOptionsBuilder {
    private DssWriteOptionsBuilder() {
    }

    static Map<String, String> buildWriteOptions(List<String> chosenSourceList, DestinationSelectionPanel destinationPanel) {
        return buildWriteOptions(chosenSourceList, destinationPanel, "PRECIPITATION");
    }

    static Map<String, String> buildWriteOptions(List<String> chosenSourceList, DestinationSelectionPanel destinationPanel, String defaultPartC) {
        Map<String, String> writeOptions = new HashMap<>();
        String destination = destinationPanel.getDestinationTextField().getText();

        if (destination.toLowerCase().endsWith(".dss")) {
            Map<String, Set<String>> pathnameParts = DssUtil.getPathnameParts(chosenSourceList);
            String partA = getSingleOrDefault(pathnameParts.get("aParts"), "*");
            String partB = getSingleOrDefault(pathnameParts.get("bParts"), "*");
            String partC = getSingleOrDefault(pathnameParts.get("cParts"), defaultPartC);
            String partF = getSingleOrDefault(pathnameParts.get("fParts"), "*");

            String dssA = destinationPanel.getFieldA().getText();
            String dssB = destinationPanel.getFieldB().getText();
            String dssC = destinationPanel.getFieldC().getText();
            String dssF = destinationPanel.getFieldF().getText();

            writeOptions.put("partA", dssA.isEmpty() ? partA : dssA);
            writeOptions.put("partB", dssB.isEmpty() ? partB : dssB);
            writeOptions.put("partC", dssC.isEmpty() ? partC : dssC);
            writeOptions.put("partF", dssF.isEmpty() ? partF : dssF);
        }

        String units = destinationPanel.getUnitsString();
        if (!units.isEmpty()) writeOptions.put("units", units);

        String dataType = destinationPanel.getDataType();
        if (dataType != null && !dataType.isEmpty()) writeOptions.put("dataType", dataType);

        return writeOptions;
    }

    private static String getSingleOrDefault(Set<String> parts, String defaultValue) {
        List<String> list = new ArrayList<>(parts);
        return (list.size() == 1) ? list.get(0) : defaultValue;
    }
}
