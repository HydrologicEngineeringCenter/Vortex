from mil.army.usace.hec.vortex.math import BatchSanitizer 
from mil.army.usace.hec.vortex import Options
import os
from glob import glob

d_files = glob(r"G:\UA\*.dss")

for dss_file in d_files:


    basin = 'MISSOURI RIVER BASIN'
    ds = 'UA_sanitized'

    output_dss = dss_file[:-4] + '_noData.dss'
    write_options = Options.create()
    write_options.add('partF', ds )
    write_options.add('partA', 'SHG')
    write_options.add('partB', basin)
    myImport = BatchSanitizer.builder()\
            .pathToInput(dss_file)\
            .selectAllVariables()\
            .minimumThreshold(-0.1)\
            .destination(output_dss)\
            .writeOptions(write_options).build()
    myImport.process() 