set "VORTEX_HOME=C:\workspace\git_clones\vortex-0.10.20-win-x64\vortex-0.10.20"
set "PATH=%VORTEX_HOME%\bin;%VORTEX_HOME%\bin\gdal;%PATH%"
set "GDAL_DRIVER_PATH=%VORTEX_HOME%\bin\gdal\gdalplugins"
set "GDAL_DATA=%VORTEX_HOME%\bin\gdal\gdal-data"
set "PROJ_LIB=%VORTEX_HOME%\bin\gdal\projlib"
set "CLASSPATH=%VORTEX_HOME%\lib\*"

C:\jython2.7.2\bin\jython.exe -J-Xmx10g -Djava.library.path=%VORTEX_HOME%\bin;%VORTEX_HOME%\bin\gdal C:\workspace\git_clones\vortex_scripting\UA_to_DSS_IMPORT\grid_to_point_Converter.py
cmd /k