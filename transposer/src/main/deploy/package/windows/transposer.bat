SET "PATH=.\bin\gdal;%PATH%"
SET "GDAL_DRIVER_PATH=.bin\gdal\gdalplugins"
SET "GDAL_DATA=.\bin\gdal\gdal-data"
SET "PROJ_LIB=.\bin\gdal\projlib"
".\jre\bin\java.exe" -Djava.library.path=".\bin;.\bin\gdal" -cp transposer.jar;lib\* transposer.TransposerWizard
