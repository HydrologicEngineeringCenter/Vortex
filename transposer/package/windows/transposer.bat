SET "PATH=.\gdal;%PATH%"
SET "GDAL_DRIVER_PATH=.\gdal\gdalplugins"
SET "GDAL_DATA=.\gdal\gdal-data"
SET "PROJ_LIB=.\gdal\projlib"
"..\jre\bin\java.exe" --module-path "..\jmods" --add-modules javafx.controls,javafx.fxml -Djava.library.path=".;.\gdal" -cp ..\lib\transposer.jar;..\lib\* transposer.TransposerWizard
