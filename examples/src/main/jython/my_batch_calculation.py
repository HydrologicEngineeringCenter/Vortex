from mil.army.usace.hec.vortex.math import BatchCalculator

path_to_input = 'C:/Temp/truckee_2016.dss'

destination = 'C:/Temp/truckee_2016_x2.dss'

write_options = {'partF': 'multiplied by 2'}

myCalculator = BatchCalculator.builder() \
    .pathToInput(path_to_input) \
    .selectAllVariables() \
    .multiplyValue(2.0) \
    .destination(destination) \
    .writeOptions(write_options) \
    .build()

myCalculator.process()
