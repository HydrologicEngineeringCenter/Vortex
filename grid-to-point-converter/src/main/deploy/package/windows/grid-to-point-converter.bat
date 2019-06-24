SET "PATH=%CD%\bin\gdal-2-4-0\bin;%PATH%"
SET "GDAL_DRIVER_PATH=%CD%\bin\gdal-2-4-0\bin\gdal\plugins"
SET "GDAL_DATA=%CD%\bin\gdal-2-4-0\bin\gdal-data"
SET "PROJ_LIB=%CD%\bin\gdal-2-4-0\bin\proj\SHARE"
"%CD%\jre\bin\java.exe" -Djava.library.path="%CD%\bin;%CD%\bin\gdal-2-4-0\bin\gdal\java" -cp grid-to-point-converter.jar;lib\* converter.ConverterWizard
