from mil.army.usace.hec.vortex.geo import BatchSubsetter

path_to_input = 'C:/Temp/truckee_2016.dss'

clip_ds = 'C:/Temp/Truckee_River_Watershed_5mi_buffer.shp'

destination = 'C:/Temp/truckee_2016_clipped.dss'

write_options = {'partF': 'clipped'}

myClip = BatchSubsetter.builder() \
    .pathToInput(path_to_input) \
    .selectAllVariables() \
    .setEnvelopeDataSource(clip_ds) \
    .destination(destination) \
    .writeOptions(write_options) \
    .build()

myClip.process()
