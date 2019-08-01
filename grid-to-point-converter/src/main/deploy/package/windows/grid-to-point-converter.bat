SET "PATH=%CD%\bin\gdal;%PATH%"
SET "GDAL_DRIVER_PATH=%CD%\bin\gdal\gdalplugins"
SET "GDAL_DATA=%CD%\bin\gdal\gdal-data"
SET "PROJ_LIB=%CD%\bin\gdal\projlib"
"%CD%\jre\bin\java.exe" -Djava.library.path="%CD%\bin;%CD%\bin\gdal" -cp grid-to-point-converter.jar;lib\* converter.ConverterWizard
