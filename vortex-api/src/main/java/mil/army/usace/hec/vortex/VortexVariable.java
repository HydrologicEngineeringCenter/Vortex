package mil.army.usace.hec.vortex;

import java.util.Arrays;

// long names taken from https://cfconventions.org/Data/cf-standard-names/current/build/cf-standard-name-table.html
public enum VortexVariable {
    PRECIPITATION("precipitation", "lwe_thickness_of_precipitation_amount","PRECIPITATION"),
    PRESSURE("pressure", "air_pressure", "PRESSURE"),
    TEMPERATURE("temperature", "air_temperature", "TEMPERATURE"),
    SOLAR_RADIATION("solar radiation", "surface_downwelling_shortwave_flux_in_air", "SOLAR RADIATION"),
    WINDSPEED("windspeed", "wind_speed", "WINDSPEED"),
    SWE("swe", "lwe_thickness_of_surface_snow_amount", "SWE"),
    SNOWFALL_ACCUMULATION("snowfall accumulation", "lwe_thickness_of_snowfall_amount", "SNOWFALL ACCUMULATION"),
    ALBEDO("albedo", "surface_albedo", "ALBEDO"),
    SNOW_DEPTH("snow depth", "surface_snow_thickness", "SNOW DEPTH"),
    LIQUID_WATER("liquid water", "lwe_thickness_of_snowfall_amount", "LIQUID WATER"),
    SNOW_SUBLIMATION("snow sublimation", "surface_snow_sublimation_amount", "SNOW SUBLIMATION"),
    COLD_CONTENT("cold content", "cold_content", "COLD CONTENT"),
    COLD_CONTENT_ATI("cold content ati", "cold_content_ati", "COLD CONTENT ATI"),
    MELTRATE_ATI("meltrate ati", "meltrate_ati", "MELTRATE ATI"),
    SNOW_MELT("snow melt", "surface_snow_melt_amount", "SNOW MELT"),
    HUMIDITY("humidity", "relative_humidity", "HUMIDITY"),
    MOISTURE_DEFICIT("moisture_deficit", "moisture_deficit_todo", "DEFICIT"),
    PERCOLATION_RATE("percolation rate", "percolation_todo", "PERCOLATION"),
    IMPERVIOUS_AREA("impervious area", "impervious_area_todo", "IMPERVIOUS"),
    SCS_CURVE_NUMBER("scs curve number", "scs_curve_number_todo", "CURVE"),
    STORAGE_CAPACITY("storage capacity", "storage_capacity_todo", "STOR-CAP"),
    STORAGE_COEFFICIENT("storage coefficient", "storage_coefficient_todo", "STOR-COEF"),
    CROP_COEFFICIENT("crop coefficient", "crop_coefficient_todo", "CROP_COEFF"),
    UNDEFINED("", "", "");

    private final String name;
    private final String longName;
    private final String dssName;

    VortexVariable(String name, String longName, String dssName) {
        this.name = name;
        this.longName = longName;
        this.dssName = dssName;
    }

    public static VortexVariable fromDssName(String dssName) {
        return Arrays.stream(VortexVariable.values())
                .filter(n -> n.dssName.equals(dssName))
                .findFirst()
                .orElse(UNDEFINED);
    }

    public String getLowerCasedVariableName() {
        return toString().toLowerCase();
    }

    public String getname() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    public String getDssName() {
        return dssName;
    }

    public static VortexVariable fromName(String name) {
        if (name == null || name.isEmpty()) return UNDEFINED;
        name = name.toLowerCase().replaceAll("\\s", "");
        if (isValidPrecipitationName(name)) return PRECIPITATION;
        if (isValidPressureName(name)) return PRESSURE;
        if (isValidTemperatureName(name)) return TEMPERATURE;
        if (isValidSolarRadiationName(name)) return SOLAR_RADIATION;
        if (isValidWindSpeedName(name)) return WINDSPEED;
        if (isValidSWEName(name)) return SWE;
        if (isValidSnowfallAccumulationName(name)) return SNOWFALL_ACCUMULATION;
        if (isValidAlbedoName(name)) return ALBEDO;
        if (isValidSnowDepthName(name)) return SNOW_DEPTH;
        if (isValidLiquidWaterName(name)) return LIQUID_WATER;
        if (isValidSnowSublimationName(name)) return SNOW_SUBLIMATION;
        if (isValidColdContentName(name)) return COLD_CONTENT;
        if (isValidColdContentATIName(name)) return COLD_CONTENT_ATI;
        if (isValidMeltrateATIName(name)) return MELTRATE_ATI;
        if (isValidSnowMeltName(name)) return SNOW_MELT;
        if (isValidHumidityName(name)) return HUMIDITY;
        if (isValidMoistureDeficitName(name)) return MOISTURE_DEFICIT;
        if (isValidPercolationName(name)) return PERCOLATION_RATE;
        if (isValidImperviousAreaName(name)) return IMPERVIOUS_AREA;
        if (isValidScsCurveNumberName(name)) return SCS_CURVE_NUMBER;
        if (isValidStorageCapacityName(name)) return STORAGE_CAPACITY;
        if (isValidStorageCoefficientName(name)) return STORAGE_COEFFICIENT;
        if (isValidCropCoefficientName(name)) return CROP_COEFFICIENT;
        return UNDEFINED;
    }

    private static boolean isValidPrecipitationName(String name) {
        return matchesDssName(name, PRECIPITATION)
                || name.equals("precipitation")
                || name.equals("precip")
                || name.contains("precip") && name.contains("rate")
                || name.contains("precip") && name.contains("inc")
                || name.contains("precipitable") && name.contains("water")
                || name.contains("qpe")
                || name.equals("var209-6")
                || name.equals("cmorph")
                || name.equals("rainfall")
                || name.equals("pcp")
                || name.equals("pr")
                || name.equals("prec")
                || name.equals("prcp")
                || name.equals("total precipitation");
    }

    private static boolean isValidPressureName(String name) {
        return matchesDssName(name, PRESSURE) || (name.contains("pressure") && name.contains("surface"));
    }

    private static boolean isValidTemperatureName(String name) {
        return matchesDssName(name, TEMPERATURE)
                || name.contains("temperature")
                || name.equals("airtemp")
                || name.equals("tasmin")
                || name.equals("tasmax")
                || name.equals("temp-air");
    }

    private static boolean isValidSolarRadiationName(String name) {
        return matchesDssName(name, SOLAR_RADIATION)
                || ((name.contains("short") && name.contains("wave") || name.contains("solar")) && name.contains("radiation"));
    }

    private static boolean isValidWindSpeedName(String name) {
        return matchesDssName(name, WINDSPEED)
                || (name.contains("wind") && name.contains("speed"));
    }

    private static boolean isValidSWEName(String name) {
        return matchesDssName(name, SWE)
                || (name.contains("snow") && name.contains("water") && name.contains("equivalent"))
                || name.equals("swe")
                || name.equals("weasd");
    }

    private static boolean isValidSnowfallAccumulationName(String name) {
        return matchesDssName(name, SNOWFALL_ACCUMULATION)
                || (name.contains("snowfall") && name.contains("accumulation"));
    }

    private static boolean isValidAlbedoName(String name) {
        return matchesDssName(name, ALBEDO)
                || name.contains("albedo");
    }

    private static boolean isValidSnowDepthName(String name) {
        return matchesDssName(name, SNOW_DEPTH)
                || (name.contains("snow") && name.contains("depth"));
    }

    private static boolean isValidLiquidWaterName(String name) {
        return matchesDssName(name, LIQUID_WATER)
                || (name.contains("snow") && name.contains("melt") && name.contains("runoff"))
                || equalsIgnoreCaseAndSpace(name, "liquid water");
    }

    private static boolean isValidSnowSublimationName(String name) {
        return matchesDssName(name, SNOW_SUBLIMATION)
                || (name.contains("snow") && name.contains("sublimation"));
    }

    private static boolean isValidColdContentName(String name) {
        return matchesDssName(name, COLD_CONTENT)
                || equalsIgnoreCaseAndSpace(name, "cold content");
    }

    private static boolean isValidColdContentATIName(String name) {
        return matchesDssName(name, COLD_CONTENT_ATI)
                || equalsIgnoreCaseAndSpace(name, "cold content ati");
    }

    private static boolean isValidMeltrateATIName(String name) {
        return matchesDssName(name, MELTRATE_ATI)
                || equalsIgnoreCaseAndSpace(name, "meltrate ati");
    }

    private static boolean isValidSnowMeltName(String name) {
        return matchesDssName(name, SNOW_MELT)
                || equalsIgnoreCaseAndSpace(name, "snow melt");
    }

    private static boolean isValidHumidityName(String name) {
        return matchesDssName(name, HUMIDITY)
                || equalsIgnoreCaseAndSpace(name, "humidity");
    }

    private static boolean isValidMoistureDeficitName(String name) {
        return matchesDssName(name, MOISTURE_DEFICIT)
                || name.contains("deficit");
    }

    private static boolean isValidPercolationName(String name) {
        return matchesDssName(name, PERCOLATION_RATE)
                || equalsIgnoreCaseAndSpace(name, "percolation rate");
    }

    private static boolean isValidImperviousAreaName(String name) {
        return matchesDssName(name, IMPERVIOUS_AREA)
                || equalsIgnoreCaseAndSpace(name, "impervious area");
    }

    private static boolean isValidScsCurveNumberName(String name) {
        return matchesDssName(name, SCS_CURVE_NUMBER)
                || equalsIgnoreCaseAndSpace(name, "curve number");
    }

    private static boolean isValidStorageCapacityName(String name) {
        return matchesDssName(name, STORAGE_CAPACITY)
                || equalsIgnoreCaseAndSpace(name, "storage capacity");
    }

    private static boolean isValidStorageCoefficientName(String name) {
        return matchesDssName(name, STORAGE_COEFFICIENT)
                || equalsIgnoreCaseAndSpace(name, "storage coefficient");
    }

    private static boolean isValidCropCoefficientName(String name) {
        return matchesDssName(name, CROP_COEFFICIENT)
                || equalsIgnoreCaseAndSpace(name, "crop coefficient");
    }

    private static boolean matchesDssName(String name, VortexVariable variable) {
        String normalizedName = normalizeString(name);
        String normalizedDSS = normalizeString(variable.getDssName());
        return normalizedName.equals(normalizedDSS);
    }

    private static boolean equalsIgnoreCaseAndSpace(String one, String two) {
        String normalizedLeft = normalizeString(one);
        String normalizedRight = normalizeString(two);
        return normalizedLeft.equals(normalizedRight);
    }

    private static String normalizeString(String name) {
        return name.toLowerCase()
                .replaceAll("\\s+", "")
                .replace("_", "");
    }
}
