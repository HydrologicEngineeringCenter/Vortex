set "VORTEX_HOME=C:\Programs\vortex-0.15.0"
set "PATH=%VORTEX_HOME%\app;%VORTEX_HOME%\app\gdal;%VORTEX_HOME%\app\netcdf;%PATH%"
set "CLASSPATH=%VORTEX_HOME%\app\*"

C:\jython2.7.2\bin\jython.exe -J-Xmx10g -Djava.library.path=%VORTEX_HOME%\app;%VORTEX_HOME%\app\gdal -Dvortex.gdal.data=%VORTEX_HOME%\app\gdal\gdal-data -Dvortex.proj.lib=%VORTEX_HOME%\app\gdal\projlib filter_no_data.py
cmd /k
