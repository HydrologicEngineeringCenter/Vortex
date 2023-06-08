SET "PATH=.\gdal;.\netcdf;%PATH%"
SET "GDAL_DATA=.\gdal\gdal-data"
SET "PROJ_LIB=.\gdal\projlib"
"..\jre\bin\java.exe" -Djava.library.path=".;.\gdal" --add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED -cp "..\lib\*" mil.army.usace.hec.vortex.ui.VortexUi -calculator
