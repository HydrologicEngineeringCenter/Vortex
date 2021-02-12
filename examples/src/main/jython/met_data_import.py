from mil.army.usace.hec.vortex.io import BatchImporter
from mil.army.usace.hec.vortex.geo import WktFactory

in_files = ['C:/Temp/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2']

variables = ['GaugeCorrQPE01H_altitude_above_msl']

clip_shp = 'C:/Temp/Truckee_River_Watershed_5mi_buffer.shp'

geo_options = {
    'pathToShp': clip_shp,
    'targetCellSize': '2000',
    'targetWkt': WktFactory.shg(),
    'resamplingMethod': 'Bilinear'
}

destination = 'C:/Temp/myPythonImport.dss'

write_options = {'partF': 'my script import'}

myImport = BatchImporter.builder() \
    .inFiles(in_files) \
    .variables(variables) \
    .geoOptions(geo_options) \
    .destination(destination) \
    .writeOptions(write_options) \
    .build()

myImport.process()
