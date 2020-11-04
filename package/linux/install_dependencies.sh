#!/bin/bash

# If script is not run as sudo, ask for sudo password to elevate access
if [ $(id -u) != 0 ]; then
    sudo "$0" "$@"
    exit $?
fi

# Check if the current distribution is Ubuntu/Debian or not
distributionName=$(cat /etc/*release | grep ID_LIKE=)
distributionId=$(cat /etc/*release | grep DISTRIB_ID=)

if [ "$distributionId" != "DISTRIB_ID=Ubuntu" ] && [ "$distributionId" != "ID_LIKE=debian" ]; then
	echo "This distribution is currently not supported"
	exit 1
fi

# Getting the latest packages
apt update -y

# Add UbuntuGIS PPA to install GDAL libraries
yes "" | add-apt-repository ppa:ubuntugis/ppa

# Update to get latest packages and install GDAL
apt update -y
apt install gdal-bin -y
apt install libgdal-java -o APT::Get::Fix-Missing=true -y