from mil.army.usace.hec.vortex.math import Normalizer
from mil.army.usace.hec.vortex.io import DataReader
from mil.army.usace.hec.vortex import Options

from java.time import ZonedDateTime
from java.time import LocalDateTime
from java.time import ZoneId
from java.time import Duration

source = "C:/Temp/qpe.dss"
normals = "C:/Temp/prism.dss"

sourceGrids = DataReader.getVariables(source)
normalGrids = DataReader.getVariables(normals)

start = ZonedDateTime.of(LocalDateTime.of(2017, 1, 1, 0, 0), ZoneId.of("UTC"))
end = ZonedDateTime.of(LocalDateTime.of(2017, 1, 3, 0, 0), ZoneId.of("UTC"))
interval = Duration.ofDays(1)

destination = 'C:/Temp/normalized.dss'

options = Options.create()
options.add('partF', 'my normalized grids')

normalizer = Normalizer.builder() \
    .startTime(start) \
    .endTime(end) \
    .interval(interval) \
    .pathToSource(source) \
    .sourceVariables(sourceGrids) \
    .pathToNormals(normals) \
    .normalsVariables(normalGrids) \
    .destination(destination) \
    .writeOptions(options) \
    .build()

normalizer.normalize()
