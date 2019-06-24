# Vortex

Vortex is a collection of data processing utilities targeted for Hydrologic Engineering Center applications, e.g. HEC-HMS.

## Releases

To download the latest release of the software see [releases](https://github.com/HydrologicEngineeringCenter/Vortex/releases).

## Development version

These instructions demonstrate how to build, test, and run the source code on your local machine.  

### Prerequisites

You will need JDK 8.  The version used for building releases is OpenJDK 8, from [Amazon Corretto](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/what-is-corretto-8.html).

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
