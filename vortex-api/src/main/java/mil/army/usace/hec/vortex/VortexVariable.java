package mil.army.usace.hec.vortex;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// long names taken from https://cfconventions.org/Data/cf-standard-names/current/build/cf-standard-name-table.html
// DSS names taken from https://www.hec.usace.army.mil/confluence/hmsdocs/hmsum/latest/shared-component-data/grid-data
public enum VortexVariable {
    PRECIPITATION("precipitation", "lwe_thickness_of_precipitation_amount","PRECIPITATION"),
    TEMPERATURE("temperature", "air_temperature", "TEMPERATURE"),
    SHORTWAVE_RADIATION("solar radiation", "surface_downwelling_shortwave_flux_in_air", "RADIATION-SHORT"),
    LONGWAVE_RADIATION("longwave radiation", "surface_downwelling_shortwave_flux_in_air", "RADIATION-LONG"),
    CROP_COEFFICIENT("crop coefficient", "crop_coefficient", "CROP COEFFICIENT"),
    STORAGE_CAPACITY("storage capacity", "storage_capacity", "STORAGE CAPACITY"),
    PERCOLATION_RATE("percolation rate", "percolation_rate", "PERCOLATION"),
    STORAGE_COEFFICIENT("storage coefficient", "storage_coefficient", "STORAGE COEFFICIENT"),
    MOISTURE_DEFICIT("moisture deficit", "moisture_deficit", "MOISTURE DEFICIT"),
    IMPERVIOUS_AREA("impervious area", "impervious_area", "IMPERVIOUS AREA"),
    CURVE_NUMBER("curve number", "curve_number", "CURVE NUMBER"),
    COLD_CONTENT("cold content", "cold_content", "COLD CONTENT"),
    COLD_CONTENT_ATI("cold content ati", "cold_content_ati", "COLD CONTENT ATI"),
    MELTRATE_ATI("meltrate ati", "meltrate_ati", "MELTRATE ATI"),
    LIQUID_WATER("liquid water", "lwe_thickness_of_snowfall_amount", "LIQUID WATER"),
    SNOW_WATER_EQUIVALENT("swe", "lwe_thickness_of_surface_snow_amount", "SWE"),
    WATER_CONTENT("water content", "water_content", "WATER CONTENT"),
    WATER_POTENTIAL("water potential", "water_potential", "WATER POTENTIAL"),
    HUMIDITY("humidity", "relative_humidity", "HUMIDITY"),
    WINDSPEED("windspeed", "wind_speed", "WINDSPEED"),
    PRESSURE("pressure", "air_pressure", "PRESSURE"),
    PRECIPITATION_FREQUENCY("precipitation_frequency", "lwe_thickness_of_precipitation_frequency_amount", "PRECIPITATION-FREQUENCY"),
    ALBEDO("albedo", "surface_albedo", "ALBEDO"),
    ENERGY("energy", "energy", "ENERGY"),
    SNOWFALL_ACCUMULATION("snowfall accumulation", "lwe_thickness_of_snowfall_amount", "SNOWFALL ACCUMULATION"),
    SNOW_DEPTH("snow depth", "surface_snow_thickness", "SNOW DEPTH"),
    SNOW_SUBLIMATION("snow sublimation", "surface_snow_sublimation_amount", "SNOW SUBLIMATION"),
    SNOW_MELT("snow melt", "surface_snow_melt_amount", "SNOW MELT"),
    UNDEFINED("", "", "");

    private final String shortName;
    private final String longName;
    private final String dssName;

    private static final Logger logger = Logger.getLogger(VortexVariable.class.getName());

    VortexVariable(String shortName, String longName, String dssName) {
        this.shortName = shortName;
        this.longName = longName;
        this.dssName = dssName;
    }

    public static VortexVariable fromDssName(String dssName) {
        return Arrays.stream(VortexVariable.values())
                .filter(n -> n.dssName.equals(dssName))
                .findFirst()
                .orElse(UNDEFINED);
    }

    public String getShortName() {
        return normalizeString(shortName);
    }

    public String getLongName() {
        return longName;
    }

    public String getDssCPart() {
        return dssName;
    }

    public static boolean isUndefined(VortexVariable variable) {
        return variable.equals(UNDEFINED);
    }

    public static VortexVariable fromNames(String... names) {
        Set<VortexVariable> matchedSet = Arrays.stream(names)
                .map(VortexVariable::fromName)
                .filter(v -> !isUndefined(v))
                .collect(Collectors.toSet());

        if (matchedSet.size() > 1) {
            logger.warning("More than 1 variable matched with names");
        }

        return matchedSet.stream().findFirst().orElse(UNDEFINED);
    }

    public static VortexVariable fromName(String name) {
        if (name == null || name.isEmpty()) return UNDEFINED;
        name = name.toLowerCase().replaceAll("\\s", "");
        if (isValidPrecipitationName(name)) return PRECIPITATION;
        if (isValidTemperatureName(name)) return TEMPERATURE;
        if (isValidShortwaveRadiationName(name)) return SHORTWAVE_RADIATION;
        if (isValidLongwaveRadiationName(name)) return LONGWAVE_RADIATION;
        if (isValidCropCoefficientName(name)) return CROP_COEFFICIENT;
        if (isValidStorageCapacityName(name)) return STORAGE_CAPACITY;
        if (isValidPercolationRateName(name)) return PERCOLATION_RATE;
        if (isValidStorageCoefficientName(name)) return STORAGE_COEFFICIENT;
        if (isValidMoistureDeficitName(name)) return MOISTURE_DEFICIT;
        if (isValidImperviousAreaName(name)) return IMPERVIOUS_AREA;
        if (isValidCurveNumberName(name)) return CURVE_NUMBER;
        if (isValidColdContentName(name)) return COLD_CONTENT;
        if (isValidColdContentATIName(name)) return COLD_CONTENT_ATI;
        if (isValidMeltrateATIName(name)) return MELTRATE_ATI;
        if (isValidLiquidWaterName(name)) return LIQUID_WATER;
        if (isValidSnowWaterEquivalentName(name)) return SNOW_WATER_EQUIVALENT;
        if (isValidWaterContentName(name)) return WATER_CONTENT;
        if (isValidWaterPotentialName(name)) return WATER_POTENTIAL;
        if (isValidHumidityName(name)) return HUMIDITY;
        if (isValidWindSpeedName(name)) return WINDSPEED;
        if (isValidPressureName(name)) return PRESSURE;
        if (isValidPrecipitationFrequencyName(name)) return PRECIPITATION_FREQUENCY;
        if (isValidAlbedoName(name)) return ALBEDO;
        if (isValidEnergyName(name)) return ENERGY;
        if (isValidSnowfallAccumulationName(name)) return SNOWFALL_ACCUMULATION;
        if (isValidSnowDepthName(name)) return SNOW_DEPTH;
        if (isValidSnowSublimationName(name)) return SNOW_SUBLIMATION;
        if (isValidSnowMeltName(name)) return SNOW_MELT;
        if (isValidHumidityName(name)) return HUMIDITY;
        return UNDEFINED;
    }

    private static boolean isValidPrecipitationName(String shortName) {
        return shortName.equals("precipitation")
                || matchesDssName(shortName, PRECIPITATION)
                || shortName.equals("precip")
                || shortName.equals("precip-inc")
                || shortName.equals("precipitationcal")
                || shortName.contains("qpe")
                || shortName.equals("var209-6")
                || shortName.equals("cmorph")
                || shortName.equals("rainfall")
                || shortName.equals("pcp")
                || shortName.equals("pr")
                || shortName.equals("prec")
                || shortName.equals("prcp")
                || shortName.contains("precip") && shortName.contains("rate")
                || shortName.contains("precipitable") && shortName.contains("water")
                || shortName.contains("total") && shortName.contains("precipitation")
                || shortName.contains("convective") && shortName.contains("rainfall");
    }

    private static boolean isValidTemperatureName(String shortName) {
        return matchesDssName(shortName, TEMPERATURE)
                || shortName.contains("temperature")
                || shortName.equals("airtemp")
                || shortName.equals("tasmin")
                || shortName.equals("tasmax")
                || shortName.equals("temp")
                || shortName.equals("temp-air");
    }

    private static boolean isValidShortwaveRadiationName(String shortName) {
        return matchesDssName(shortName, SHORTWAVE_RADIATION)
                || (shortName.contains("shortwave") && shortName.contains("radiation"))
                || ((shortName.contains("solar") && shortName.contains("radiation")));
    }

    private static boolean isValidLongwaveRadiationName(String shortName) {
        return shortName.matches("long.*radiation");
    }

    private static boolean isValidCropCoefficientName(String shortName) {
        return matchesDssName(shortName, CROP_COEFFICIENT) || equalsIgnoreCaseAndSpace(shortName, "crop coefficient");
    }

    private static boolean isValidStorageCapacityName(String shortName) {
        return matchesDssName(shortName, STORAGE_CAPACITY) || equalsIgnoreCaseAndSpace(shortName, "storage capacity");
    }

    private static boolean isValidPercolationRateName(String shortName) {
        return shortName.equals("percolation") || shortName.matches("percolation\\s?rate");
    }

    private static boolean isValidStorageCoefficientName(String shortName) {
        return matchesDssName(shortName, STORAGE_COEFFICIENT) || equalsIgnoreCaseAndSpace(shortName, "storage coefficient");
    }

    private static boolean isValidMoistureDeficitName(String shortName) {
        return shortName.matches("moisture\\s?deficit");
    }

    private static boolean isValidImperviousAreaName(String shortName) {
        return shortName.matches("impervious\\s?area");
    }

    private static boolean isValidCurveNumberName(String shortName) {
        return shortName.matches("curve\\s?number");
    }

    private static boolean isValidColdContentName(String shortName) {
        return matchesDssName(shortName, COLD_CONTENT) || equalsIgnoreCaseAndSpace(shortName, "cold content");
    }

    private static boolean isValidColdContentATIName(String shortName) {
        return matchesDssName(shortName, COLD_CONTENT_ATI) || equalsIgnoreCaseAndSpace(shortName, "cold content ati");
    }

    private static boolean isValidMeltrateATIName(String shortName) {
        return matchesDssName(shortName, MELTRATE_ATI) || equalsIgnoreCaseAndSpace(shortName, "meltrate ati");
    }

    private static boolean isValidLiquidWaterName(String shortName) {
        return matchesDssName(shortName, LIQUID_WATER)
                || (shortName.contains("snow") && shortName.contains("melt") && shortName.contains("runoff"))
                || equalsIgnoreCaseAndSpace(shortName, "liquid water");
    }

    private static boolean isValidSnowWaterEquivalentName(String shortName) {
        return matchesDssName(shortName, SNOW_WATER_EQUIVALENT)
                || shortName.contains("snow") && shortName.contains("water") && shortName.contains("equivalent")
                || shortName.equals("swe")
                || shortName.equals("weasd");
    }

    private static boolean isValidWaterContentName(String shortName) {
        return shortName.contains("water") && shortName.contains("content");
    }

    private static boolean isValidWaterPotentialName(String shortName) {
        return shortName.contains("water") && shortName.contains("potential");
    }

    private static boolean isValidHumidityName(String shortName) {
        return matchesDssName(shortName, HUMIDITY) || shortName.contains("humidity");
    }

    private static boolean isValidWindSpeedName(String shortName) {
        return matchesDssName(shortName, WINDSPEED) || (shortName.contains("wind") && shortName.contains("sp"));
    }

    private static boolean isValidPressureName(String shortName) {
        return matchesDssName(shortName, PRESSURE) || (shortName.contains("pressure") && shortName.contains("surface"));
    }

    private static boolean isValidPrecipitationFrequencyName(String shortName) {
        return shortName.contains("precipitation") && shortName.contains("frequency");
    }

    private static boolean isValidAlbedoName(String shortName) {
        return matchesDssName(shortName, ALBEDO) || shortName.contains("albedo");
    }

    private static boolean isValidEnergyName(String shortName) {
        return shortName.equals("albedo");
    }

    private static boolean isValidSnowfallAccumulationName(String shortName) {
        return matchesDssName(shortName, SNOWFALL_ACCUMULATION) || (shortName.contains("snowfall") && shortName.contains("accumulation"));
    }

    private static boolean isValidSnowDepthName(String shortName) {
        return matchesDssName(shortName, SNOW_DEPTH) || (shortName.contains("snow") && shortName.contains("depth"));
    }

    private static boolean isValidSnowSublimationName(String shortName) {
        return matchesDssName(shortName, SNOW_SUBLIMATION) || (shortName.contains("snow") && shortName.contains("sublimation"));
    }

    private static boolean isValidSnowMeltName(String shortName) {
        return matchesDssName(shortName, SNOW_MELT) || equalsIgnoreCaseAndSpace(shortName, "snow melt");
    }

    private static boolean matchesDssName(String name, VortexVariable variable) {
        String normalizedName = normalizeString(name);
        String normalizedDSS = normalizeString(variable.getDssCPart());
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
