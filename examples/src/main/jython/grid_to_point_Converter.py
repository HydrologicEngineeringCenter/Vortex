from mil.army.usace.hec.vortex.convert import GridToPointConverter
from mil.army.usace.hec.vortex import Options
from mil.army.usace.hec.vortex.io import DataReader
import os
from glob import glob
from java.nio.file import Path
from java.nio.file import Paths

#DSS Grid Files to convert to time series
d_files = glob(r"G:\UA\*_noData.dss")

#Output DSS File
output_dss = Paths.get(r"G:\UA\ts\UA_SWE_Depth_MoRiverBasin.dss")

#Shapefile
clip_shp = Paths.get("C:\workspace\Mo River\shp\MissouriRiverBasin_alb.shp")

#Shapefile attribute for zonal statistics
name = 'NAME'

#Output DSS file path partA
basin = 'MISSOURI RIVER BASIN'
ds = 'UA_sanitized'

#Loop through each dss file
for dss_file in d_files:

    #Get dss pathnames
    sourceGrids = DataReader.getVariables(dss_file)

    #Output DSS wite options
    write_options = Options.create()
    write_options.add('partF', ds )
    write_options.add('partA', 'SHG')
    write_options.add('partB', basin)

    #Convert the Data
    myImport = GridToPointConverter.builder()\
            .pathToGrids(dss_file)\
            .variables(sourceGrids)\
            .pathToFeatures(clip_shp)\
            .field(name)\
            .destination(output_dss)\
            .writeOptions(write_options).build()
    myImport.convert()
