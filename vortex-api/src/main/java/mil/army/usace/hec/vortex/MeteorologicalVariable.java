package mil.army.usace.hec.vortex;

import java.util.Arrays;

public enum MeteorologicalVariable {
    PRECIPITATION("precipitation", "lwe_thickness_of_precipitation_amount","PRECIPITATION"),
    PRESSURE("pressure", "air_pressure", "PRESSURE"),
    TEMPERATURE("temperature", "air_temperature", "TEMPERATURE"),
    SOLAR_RADIATION("solar radiation", "surface_downwelling_shortwave_flux_in_air", "SOLAR RADIATION"),
    WINDSPEED("windspeed", "wind_speed", "WINDSPEED"),
    SWE("swe", "lwe_thickness_of_surface_snow_amount", "SWE"),
    SNOWFALL_ACCUMULATION("snowfall accumulation", "lwe_thickness_of_snowfall_amount", "SNOWFALL ACCUMULATION"),
    ALBEDO("albedo", "surface_albedo", "ALBEDO"),
    SNOW_DEPTH("snow depth", "", "SNOW DEPTH"), // Not sure
    LIQUID_WATER("liquid water", "", "LIQUID WATER"), // Not sure
    SNOW_SUBLIMATION("snow sublimation", "", "SNOW SUBLIMATION"), // Not sure
    COLD_CONTENT("cold content", "", "COLD CONTENT"), // Not sure
    COLD_CONTENT_ATI("cold content ati", "", "COLD CONTENT ATI"), // Not sure
    MELTRATE_ATI("meltrate ati", "", "MELTRATE ATI"), // Not sure
    SNOW_MELT("snow melt", "", "SNOW MELT"), // Not sure
    UNKNOWN("", "", "");

    private final String shortName;
    private final String longName;
    private final String dssName;

    MeteorologicalVariable(String shortName, String longName, String dssName) {
        this.shortName = shortName;
        this.longName = longName;
        this.dssName = dssName;
    }

    public static MeteorologicalVariable fromDssName(String dssName) {
        return Arrays.stream(MeteorologicalVariable.values())
                .filter(n -> n.dssName.equals(dssName))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public String getDssName() {
        return dssName;
    }

    public static MeteorologicalVariable fromName(String name) {
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
        return UNKNOWN;
    }

    private static boolean isValidPrecipitationName(String shortName) {
        return shortName.contains("precipitation")
                || shortName.contains("precip")
                || shortName.contains("precip") && shortName.contains("rate")
                || shortName.contains("qpe01h")
                || shortName.contains("var209-6")
                || shortName.contains("rainfall")
                || shortName.equals("pr");
    }

    private static boolean isValidPressureName(String shortName) {
        return shortName.contains("pressure") && shortName.contains("surface");
    }

    private static boolean isValidTemperatureName(String shortName) {
        return shortName.contains("temperature")
                || shortName.equals("airtemp")
                || shortName.equals("tasmin")
                || shortName.equals("tasmax")
                || shortName.equals("temp-air");
    }

    private static boolean isValidSolarRadiationName(String shortName) {
        return (shortName.contains("short") && shortName.contains("wave") || shortName.contains("solar"))
                && shortName.contains("radiation");
    }

    private static boolean isValidWindSpeedName(String shortName) {
        return shortName.contains("wind") && shortName.contains("speed");
    }

    private static boolean isValidSWEName(String shortName) {
        return shortName.contains("snow") && shortName.contains("water") && shortName.contains("equivalent")
                || shortName.equals("swe");
    }

    private static boolean isValidSnowfallAccumulationName(String shortName) {
        return shortName.contains("snowfall") && shortName.contains("accumulation");
    }

    private static boolean isValidAlbedoName(String shortName) {
        return shortName.contains("albedo");
    }

    private static boolean isValidSnowDepthName(String shortName) {
        return shortName.contains("snow") && shortName.contains("depth");
    }

    private static boolean isValidLiquidWaterName(String shortName) {
        return shortName.contains("snow") && shortName.contains("melt") && shortName.contains("runoff")
                || shortName.equals("liquid water");
    }

    private static boolean isValidSnowSublimationName(String shortName) {
        return shortName.contains("snow") && shortName.contains("sublimation");
    }

    private static boolean isValidColdContentName(String shortName) {
        return shortName.equals("cold content");
    }

    private static boolean isValidColdContentATIName(String shortName) {
        return shortName.equals("cold content ati");
    }

    private static boolean isValidMeltrateATIName(String shortName) {
        return shortName.equals("meltrate ati");
    }

    private static boolean isValidSnowMeltName(String shortName) {
        return shortName.equals("snow melt");
    }
}
