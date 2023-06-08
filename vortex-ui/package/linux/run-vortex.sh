#!/bin/bash

if [ -z "$1" ]; then
  echo "No argument supplied. Please provide either:"
  echo "- '-calculator' to perform multiplication, division, addition, or subtraction to a series of grids."
  echo "- '-clipper' to clip grids to a shapefile or other geometry layer."
  echo "- '-grid-to-point' to convert gridded data to time-series data for a polygon or a point."
  echo "- '-image-exporter' to export gridded data to GeoTIFF or ASC raster formats."
  echo "- '-importer' to import NetCDF, Grib, HDF, ASC, or HEC-DSS files, with options for clipping, re-projecting, and resampling data, and writes to HEC-DSS format."
  echo "- '-normalizer' to normalize volumes from one set of grids to another, e.g. normalize hourly QPE grids to daily PRISM grids."
  echo "- '-sanitizer' to screen and replace values above or below a threshold, with an option to override DSS grid units."
  echo "- '-shifter' to rotate and shift grids spatially, with a scale parameter to scale grid values."
  echo "- '-time-shifter' to shift grids in time."
  exit 1
fi

export GDAL_DATA="../bin/gdal/gdal-data"
export PROJ_LIB="../bin/gdal/proj"

JAVA_PATH="../jre/Contents/Home/bin/java"
JAVA_LIB_PATH="../bin:../bin/gdal:/usr/lib/jni"
ADD_OPENS="java.desktop/sun.awt.shell=ALL-UNNAMED"
CLASS_PATH="../lib/*"
MAIN_CLASS="mil.army.usace.hec.vortex.ui.VortexUi"
APP_ARG="$1"

"$JAVA_PATH" -Djava.library.path="$JAVA_LIB_PATH" --add-opens="$ADD_OPENS" -cp "$CLASS_PATH" "$MAIN_CLASS" "$APP_ARG"