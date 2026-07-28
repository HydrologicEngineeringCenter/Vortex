# Toolchain image for the Linux CI builds.
#
# This is built on the agent by the TeamCity "Build the Linux toolchain image"
# step and referenced by tag, so the image is never pushed to or pulled from a
# registry and this file is the only source of truth for what Linux builds run
# against. That matters because the alternative -- installing a toolchain on
# each agent -- is what produced builds that passed on nebula-linux-01 and
# failed on -02 and -03 with the same commit.
#
# The base image is still pulled, so point BASE_IMAGE at an internal mirror if
# Docker Hub is unreachable from the agents.
ARG BASE_IMAGE=eclipse-temurin:21-jdk
FROM ${BASE_IMAGE}

# ca-certificates is the reason this image exists. Gradle resolves
# edu.ucar:netcdf4 and edu.ucar:cdm-core from artifacts.unidata.ucar.edu, which
# is the only source for them -- they are absent from Maven Central and from the
# HEC Nexus. That host is served under the Sectigo Public Server Authentication
# Root R46, and a JDK whose trust store predates it fails resolution with
# "PKIX path building failed" rather than falling through to another repository.
# Refreshing the bundle here fixes it identically on every agent.
#
# git and tar are needed by the build itself: nebula.release reads git history
# to infer the version, and getNatives extracts the native libraries from
# tar.gz archives.
#
# libnetcdf-dev supplies the netCDF *C* library. netcdf-java writes NETCDF4 by
# calling into it through JNA, and without it a write silently produces a
# zero-length file -- the reader then fails with EOFException rather than
# anything naming the real cause. Windows gets this from the
# edu.ucar:netcdf:4.9.2:win-x64 archive that getNatives extracts onto PATH;
# there is no equivalent Linux or macOS artifact published in the HEC Nexus, so
# on Linux it comes from the distribution instead. The -dev package is the one
# to install because it provides the unversioned libnetcdf.so symlink that JNA
# resolves when asked for "netcdf"; the runtime package alone leaves only
# libnetcdf.so.N, which JNA does not reliably find.
#
# Note this fixes the container, and so CI, but not a developer running the
# tests directly on a Linux host -- they would need the same package installed.
RUN apt-get update \
    && apt-get install --no-install-recommends --yes \
        ca-certificates \
        git \
        libnetcdf-dev \
        tar \
    && update-ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# The container runs as root while the checkout it is given is owned by the
# agent user, and git refuses to operate on a repository owned by someone else
# ("detected dubious ownership"). nebula.release reads git history to infer the
# version, and the release build also pushes a tag, so every build here is a git
# consumer and would fail on that check. Trusting any path is acceptable in a
# throwaway build container whose only repository is the one just checked out.
RUN git config --system --add safe.directory '*'

# nebula.release creates an annotated tag, and an annotated tag records a tagger,
# so it refuses to run at all without an identity -- "Git user.name is not set",
# raised while applying the plugin, before any task. The agent has one; this
# container does not, and the release build runs here. A service identity rather
# than a person's: the tag is made by the pipeline, and GitHub already attributes
# the push itself to the account whose token the VCS root carries.
#
# Set globally as well as system-wide. nebula reads git through JGit, which finds
# $HOME/.gitconfig reliably and is less predictable about /etc/gitconfig, and the
# cost of a redundant config file is nothing next to another release cycle spent
# discovering it did not take.
RUN git config --system user.name "Vortex CI" \
    && git config --system user.email "vortex-ci@hecdev.net" \
    && git config --global user.name "Vortex CI" \
    && git config --global user.email "vortex-ci@hecdev.net"

# Gradle is supplied by the wrapper in the repository, so nothing to install.
# JAVA_HOME comes from the base image, which is why the TeamCity Gradle runner
# leaves jdkHome empty for the containerised steps.
