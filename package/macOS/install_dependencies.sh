#!/bin/bash

/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"
brew tap osgeo/osgeo4mac
brew install pkg-config armadillo ant curl-openssl expat freexl geos giflib json-c mdbtools numpy libiconv osgeo-libkml libpq osgeo-libspatialite libzip pcre openssl qhull sfcgal sqlite swig zlib cfitsio epsilon osgeo-hdf4 hdf5 jpeg-turbo libdap libpng libxml2 webp zstd unixodbc xerces-c xz osgeo-proj osgeo-postgresql
brew install osgeo-netcdf