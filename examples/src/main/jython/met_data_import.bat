set "VORTEX_HOME=C:\Programs\vortex-0.8.7"
set "PATH=%VORTEX_HOME%\bin;%VORTEX_HOME%\bin\gdal;%PATH%"
set "GDAL_DRIVER_PATH=%VORTEX_HOME%\bin\gdal\gdalplugins"
set "GDAL_DATA=%VORTEX_HOME%\bin\gdal\gdal-data"
set "PROJ_LIB=%VORTEX_HOME%\bin\gdal\projlib"
set "CLASSPATH=%VORTEX_HOME%\lib\*"
C:\jython2.7.1\bin\jython.exe -Djava.library.path=%VORTEX_HOME%\bin;%VORTEX_HOME%\bin\gdal met_data_import.py