set "VORTEX_HOME=C:\Programs\vortex-0.10.11"
set "PATH=%VORTEX_HOME%\bin;%VORTEX_HOME%\bin\gdal;%PATH%"
set "GDAL_DRIVER_PATH=%VORTEX_HOME%\bin\gdal\gdalplugins"
set "GDAL_DATA=%VORTEX_HOME%\bin\gdal\gdal-data"
set "PROJ_LIB=%VORTEX_HOME%\bin\gdal\projlib"
set "CLASSPATH=%VORTEX_HOME%\lib\*"
C:\Programs\jython2.7.2\bin\jython.exe -J-Xmx2g -Djava.library.path=%VORTEX_HOME%\bin;%VORTEX_HOME%\bin\gdal normalize.py