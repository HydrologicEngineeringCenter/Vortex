<img src="importer/src/main/resources/vortex_black.png" alt="[logo]" width="32"/> Vortex
=======================

[![DOI](https://zenodo.org/badge/193537999.svg)](https://zenodo.org/badge/latestdoi/193537999)

Vortex is a collection of data processing utilities targeted for Hydrologic Engineering Center applications, e.g. HEC-HMS. The Vortex API uses [NetCDF Java](https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/) and [GDAL](https://gdal.org/) libraries to perform operations on spatial data.

The **importer** utility takes NetCDF, Grib, HDF, ASC, or HEC-DSS files as input, gives the user options for clipping, re-projecting, and resampling data, and writes to HEC-DSS format.

The **sanitizer** utility screens and replaces values above or below a threshold. There is also an option to override DSS grid units.

The **clipper** utility clips grids to a shapefile or other geometry layer. 

The **grid-to-point-converter** utility converts gridded data to basin-average time-series data.

The **image-exporter** utility exports gridded data to GeoTIFF or ASC raster formats.

The **transposer** utility rotates and shifts grids spatially. The utility has a scale parameter to scale grid values.

The **time-shifter** utility shifts grids in time.

The **normalizer** utility normalizes volumes from one set of grids to another, e.g. normalize hourly QPE grids to daily [PRISM](http://www.prism.oregonstate.edu/) grids.

## Release downloads

To download the latest release of the software see [releases](https://github.com/HydrologicEngineeringCenter/Vortex/releases).

For sample data retrieval scripts for QPE, RTMA, and HRRR data, see the [data retrieval scripts](https://github.com/HydrologicEngineeringCenter/data-retrieval-scripts) repository.

## Scripting

The Vortex API can be called via jython [scripts](https://github.com/HydrologicEngineeringCenter/Vortex/wiki/Vortex-scripting-example) for batch processing.

## Building from source

These instructions demonstrate how to build, test, and run the source code on your local machine.  

### Prerequisites

You will need JDK 11.  The version used for building releases is OpenJDK, from [AdoptOpenJDK](https://adoptopenjdk.net/releases.html). When running from an IDE, you must also include a JavaFX SDK in your run configuration (https://gluonhq.com/products/javafx/). You do not need a JavaFX SDK if running via Gradle.

This repository includes a [Gradle](https://gradle.org/) Wrapper; No Gradle installation is required. The JAVA_HOME environment variable should be set to a project appropriate JDK.

### Build the project

To build the project use the gradle build command:

```bat
gradlew build
```

### Running tests

To run tests use the gradle test command:

```bat
gradlew test
```

### Running a utility

To run a utility use the gradle run command:

```bat
gradlew importer:run
```

## Contributing

See [CONTRIBUTING.md](https://github.com/HydrologicEngineeringCenter/Vortex/blob/master/CONTRIBUTING.md) for details.

## Versioning

See [SemVer](http://semver.org/). 

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
