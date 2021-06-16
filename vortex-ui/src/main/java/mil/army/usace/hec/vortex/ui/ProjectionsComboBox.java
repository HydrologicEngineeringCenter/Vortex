package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.geo.WktFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectionsComboBox extends JComboBox<String> {

    public ProjectionsComboBox() {
        ArrayList<String> utmZones = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            utmZones.add("UTM " + i + " N");
            utmZones.add("UTM " + i + " S");
        }

        List<String> projections = new ArrayList<>();
        projections.add("SHG");
        projections.addAll(utmZones);

        setModel(new DefaultComboBoxModel<>(projections.toArray(String[]::new)));
        setSelectedItem("SHG");
    }

    public int getEpsg() {
        String selection = String.valueOf(getSelectedItem());
        return getEpsg(selection);
    }

    public static int getEpsg(String displayString) {
        if (displayString.equals("SHG")) {
            return 5070;
        } else {
            int zone = Integer.parseInt(displayString.substring(4, displayString.length() - 2));
            String hemisphereString = displayString.substring(displayString.length() - 1);
            if (hemisphereString.equals("N")) {
                return Integer.parseInt("326" + String.format("%02d", zone));
            } else {
                return Integer.parseInt("327" + String.format("%02d", zone));
            }
        }
    }

    public String getWkt() {
        int epsg = getEpsg();
        return WktFactory.fromEpsg(epsg);
    }

    public static String getDisplayString(int epsgCode) {
        if (epsgCode == 0 || epsgCode == 5070)
            return "SHG";

        String epsgString = Integer.toString(epsgCode);
        int thirdDigit = Character.getNumericValue(epsgString.charAt(2));
        String zone = epsgString.substring(epsgString.length() - 2).replaceFirst("^0+(?!$)", "");
        if (thirdDigit == 6)
            return "UTM " + zone + " N";

        if (thirdDigit == 7)
            return "UTM " + zone + " S";

        return "SHG";
    }
}
