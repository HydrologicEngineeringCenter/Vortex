<img src="importer/src/main/resources/vortex_black.png" alt="[logo]" width="32"/> Vortex
=======================

Vortex is a collection of data processing utilities targeted for Hydrologic Engineering Center applications, e.g. HEC-HMS. The Vortex API uses [NetCDF Java](https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/) and [GDAL](https://gdal.org/) libraries to perform operations on spatial data.

The **importer** utility takes NetCDF, Grib, HDF, ASC, or HEC-DSS files as input, gives the user options for clipping, re-projecting, and resampling data, and writes to HEC-DSS format. 

The **grid-to-point-converter** utility converts gridded data to basin-average time-series data.

The **time-shifter** utility shifts grids in time.

The **normalizer** utility normalizes volumes from one set of grids to another, e.g. normalize hourly QPE grids to daily [PRISM](http://www.prism.oregonstate.edu/) grids.

## Release downloads

To download the latest release of the software see [releases](https://github.com/HydrologicEngineeringCenter/Vortex/releases).

## Building from source

These instructions demonstrate how to build, test, and run the source code on your local machine.  

### Prerequisites

You will need JDK 8.  The version used for building releases is OpenJDK 8, from [Amazon Corretto](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/what-is-corretto-8.html). The [AdoptOpenJDK](https://adoptopenjdk.net/) does not include the JavaFX library; Attempts to build with AdoptOpenJDK will fail to compile.

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
