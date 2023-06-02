SET "PATH=.\gdal;.\netcdf;%PATH%"
SET "GDAL_DATA=.\gdal\gdal-data"
SET "PROJ_LIB=.\gdal\projlib"
"..\jre\bin\java.exe" --module-path "..\jmods" --add-modules javafx.controls,javafx.fxml -Djavafx.cachedir=. -Djava.library.path=".;.\gdal" -cp ..\lib\grid-to-point-converter.jar;..\lib\* converter.ConverterWizard
