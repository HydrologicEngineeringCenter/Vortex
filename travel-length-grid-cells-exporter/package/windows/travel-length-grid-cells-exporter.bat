SET "PATH=.\gdal;%PATH%"
SET "GDAL_DRIVER_PATH=.\gdal\gdalplugins"
SET "GDAL_DATA=.\gdal\gdal-data"
SET "PROJ_LIB=.\gdal\projlib"
"..\jre\bin\not-java.exe" -Djava.library.path=".;.\gdal" -cp ..\lib\travel-length-grid-cells-exporter.jar;..\lib\* exporter.TravelLengthGridCellsExporterWizard