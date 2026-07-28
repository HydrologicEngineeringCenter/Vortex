#!/bin/bash

# =============================================================================
# Build GDAL 3.2.1 with Java bindings for macOS, packaged for Vortex.
# =============================================================================
#
# Produces gdal-3.2.1-macOS-<arch>.zip laid out the way getNatives expects: a
# single top-level gdal/ directory holding libgdalalljni.dylib and every dylib
# it needs, each rewritten to load its siblings through @loader_path so the
# directory can be unpacked anywhere. That matches the existing
# org.gdal:gdal:3.5.0_1:macOS-x64 artifact, which this is meant to replace.
#
# WHY 3.2.1
#   Windows and Linux build against native GDAL 3.2.1 and their regression
#   expectations were recorded there. macOS runs 3.5.0_1, and three tests
#   disagree because of it: TransposerTest.TransposeFtWorthGrid expects a NaN
#   cell where 3.5 produces a value, and the two MrmsPrecipPassesRegression
#   tests expect a maximum of 1.204 where 3.5 gives 1.366. No macOS 3.2 build
#   is published anywhere, hence this script.
#
# PREREQUISITES
#   xcode-select --install
#   Homebrew                     https://brew.sh
#   A JDK (JAVA_HOME must be set; JDK 21 matches the build agents)
#
# Everything else -- autoconf, automake, libtool, pkg-config, swig, ant and the
# libraries GDAL links against -- the script installs through Homebrew itself.
# That does modify shared state on a build agent, which is the trade for not
# needing the machine provisioned by hand before every run.
#
# RUN IT
#   mkdir -p ~/gdal-build && cd ~/gdal-build
#   /path/to/build-gdal-macos.sh
#
# The build root must not contain spaces.
#
# -----------------------------------------------------------------------------
# STATE
# -----------------------------------------------------------------------------
# The build itself works: PROJ and GDAL compile, the Java bindings build, and
# the bundle packages. What is not yet proven is a bundle Vortex can actually
# use, because build 8 packaged one with no netCDF driver and called it a
# success.
#
# That is worth understanding before changing anything here. brew install failed
# for netcdf; the script reported it as "may already be present" and discarded
# brew's output; /usr/local/opt/netcdf therefore did not exist; and GDAL's
# configure, which disables an optional dependency whose path does not pan out
# and carries on, reported "NetCDF support: no" and exited zero. Three tolerant
# steps in a row, each defensible alone, adding up to a confidently green build
# of the wrong thing. The guards against that -- fatal brew failures, prefixes
# resolved through brew rather than assumed, and configure's summary read back
# and checked -- are the reason those parts look defensive.
#
# HDF5, Poppler, OpenJPEG and LibKML are off deliberately, following the
# reference script, which had to disable them at GDAL 3.9.1 because 2021 sources
# do not compile against current headers. The artifact being replaced does ship
# libhdf5 and libopenjp2, so if a format turns out to need them that is the
# first thing to revisit -- it is a decision, not an oversight.
#
# Java bindings on macOS are the known-fragile part generally. See
# https://github.com/OSGeo/gdal/issues/10316 -- they fail to build from source
# for GDAL 3.7 and newer. 3.2 predates that report and does build here.
# =============================================================================

set -euo pipefail

# --- Configuration -----------------------------------------------------------

# Match the native GDAL used on Windows and Linux.
GDAL_TAG="v3.2.1"

# GDAL 3.2 requires PROJ >= 6. 7.2.1 is contemporary with GDAL 3.2.1; a much
# newer PROJ will not compile against it.
PROJ_TAG="7.2.1"

# build-macOS, the agent that runs the macOS builds, is Intel. Set to
# "x86_64;arm64" for a universal build that would also suit mac-studio-01-vm.
ARCH="${ARCH:-x86_64}"

BUILD_TYPE="Release"
JOBS="$(sysctl -n hw.ncpu)"
ROOT="$(pwd)"
OUT="$ROOT/Output"

GDAL_GH_URL="https://github.com/OSGeo/gdal"
PROJ_GH_URL="https://github.com/OSGeo/PROJ"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Where a formula actually lives. Asking brew beats assuming <prefix>/opt/<name>,
# which is only right for the common case and silently wrong for keg-only or
# versioned formulae -- and a wrong path here does not fail, it just removes a
# driver from the build.
formula_prefix() { brew --prefix "$1" 2>/dev/null || true; }

# Resolve a formula to a prefix and confirm the library is really in it. GDAL's
# configure disables an optional dependency whose path does not pan out and
# carries on, so this is the last point where a missing one can still be an
# error rather than a quietly absent driver.
require_prefix() {
  local pkg="$1" p
  p="$(formula_prefix "$pkg")"
  if [ -z "$p" ] || [ ! -d "$p/lib" ]; then
    log_error "$pkg has no usable prefix (brew --prefix returned '${p:-nothing}')."
    return 1
  fi
  printf '%s' "$p"
}

# --- Preflight ---------------------------------------------------------------

preflight() {
  log_info "Checking prerequisites..."

  # CMake and configure both mishandle install prefixes containing spaces.
  case "$ROOT" in
    *" "*) log_error "Build path contains spaces: $ROOT"; exit 1 ;;
  esac

  xcode-select -p &>/dev/null || {
    log_error "No developer directory selected: xcode-select --install"; exit 1; }

  # xcode-select -p succeeding is not the same as having the Command Line Tools.
  # It reports whichever developer directory is selected, which on build-macOS is
  # Xcode.app -- and Homebrew's gcc bottle refuses that in as many words:
  # "the bottle needs the Xcode Command Line Tools to be installed at
  # /Library/Developer/CommandLineTools. Development tools provided by Xcode.app
  # are not sufficient." netcdf depends on gcc for gfortran, so on an agent
  # without them netCDF cannot be installed at all. Build 9 failed here.
  #
  # A warning rather than an error: if netcdf is already installed, brew is never
  # asked to build it and none of this matters.
  [ -d /Library/Developer/CommandLineTools ] || {
    log_warn "No Command Line Tools at /Library/Developer/CommandLineTools."
    log_warn "Homebrew bottles that require them -- gcc, and through it netcdf --"
    log_warn "will refuse to install. Fix on this machine with:"
    log_warn "    xcode-select --install"; }
  command -v brew &>/dev/null || {
    log_error "Homebrew missing: https://brew.sh"; exit 1; }

  # The Java bindings are the whole point of this build, so fail early and
  # loudly rather than producing a bundle without libgdalalljni.dylib.
  [ -n "${JAVA_HOME:-}" ] || {
    log_error "JAVA_HOME is not set. The Java bindings cannot be built without it."
    log_error "  export JAVA_HOME=\$(/usr/libexec/java_home -v 21)"; exit 1; }
  [ -x "$JAVA_HOME/bin/javac" ] || {
    log_error "No javac at \$JAVA_HOME/bin/javac ($JAVA_HOME)"; exit 1; }

  log_info "Prerequisites satisfied. JAVA_HOME=$JAVA_HOME"
}

# Build tools and libraries both come from Homebrew, so install rather than
# demand them: the build agent had Homebrew and a JDK but none of the autotools,
# and requiring someone to provision the machine by hand each time is how a job
# like this rots. Note this does modify shared agent state.
install_deps() {
  # Homebrew is extremely noisy by default. The first run of this on the build
  # agent produced a log over 200,000 characters -- mostly brew auto-updating
  # and then printing its entire cask catalogue when a name did not resolve --
  # which is past what the TeamCity log API will return, so the actual error
  # could not be read at all. These three settings are what make the log usable:
  #   --formula   stops brew searching casks, which is where the catalogue dump
  #               comes from, and none of these are casks
  #   --quiet     drops the per-formula pour/link chatter
  #   NO_AUTO_UPDATE  stops brew updating itself on every invocation, which is
  #               both slow and loud
  export HOMEBREW_NO_AUTO_UPDATE=1
  export HOMEBREW_NO_ENV_HINTS=1

  # Install one formula, and be able to say afterwards whether it is actually
  # there. brew exits non-zero both when a formula is already installed and when
  # installing it fails, so the exit code alone cannot separate the two. The
  # first version of this treated every failure as "may already be present" and
  # discarded brew's output, which is how build 8 got through with no netCDF: the
  # install failed, the warning read as benign, and the reason was thrown away.
  # Ask brew what is installed, and keep the output when it genuinely fails.
  brew_require() {
    local pkg="$1" out
    if brew list --formula --versions "$pkg" >/dev/null 2>&1; then
      log_info "  $pkg (already installed)"
      return 0
    fi
    if out="$(brew install --formula --quiet "$pkg" 2>&1)"; then
      log_info "  $pkg"
      return 0
    fi
    log_error "  $pkg -- brew install failed:"
    printf '%s\n' "$out" | tail -25 | sed 's/^/        /'
    return 1
  }

  # One at a time rather than one invocation with every name: brew aborts the
  # whole batch on a name it cannot resolve, so a single bad formula would
  # silently leave everything after it uninstalled.
  local pkg
  local failed=()

  log_info "Installing build tools..."
  for pkg in autoconf automake libtool pkg-config swig ant; do
    brew_require "$pkg" || failed+=("$pkg")
  done

  log_info "Installing libraries..."
  for pkg in sqlite libtiff libgeotiff jpeg-turbo libpng geos zstd netcdf; do
    brew_require "$pkg" || failed+=("$pkg")
  done

  if [ "${#failed[@]}" -gt 0 ]; then
    log_error "Could not install: ${failed[*]}"
    log_error "Every one of these is required. netCDF in particular is not"
    log_error "optional -- the artifact this replaces ships libnetcdf.dylib, and"
    log_error "GDAL will drop the driver and build cleanly without it."
    case " ${failed[*]} " in
      *" netcdf "*)
        if [ ! -d /Library/Developer/CommandLineTools ]; then
          log_error ""
          log_error "netcdf depends on gcc for gfortran, and the gcc bottle will not"
          log_error "install without the Command Line Tools at"
          log_error "/Library/Developer/CommandLineTools, which this machine lacks."
          log_error "That is almost certainly the whole of it. Run:"
          log_error "    xcode-select --install"
        fi ;;
    esac
    exit 1
  fi

  # Tools have to be on PATH, not merely installed: an unlinked keg satisfies
  # brew list and still leaves nothing to run.
  local missing=()
  for tool in swig ant autoconf automake libtool pkg-config; do
    command -v "$tool" &>/dev/null || missing+=("$tool")
  done
  if [ "${#missing[@]}" -gt 0 ]; then
    log_error "Installed but not on PATH: ${missing[*]}"
    log_error "Try 'brew link ${missing[*]}' on this machine, then re-run."
    exit 1
  fi

  # GDAL 3.2 was released against SWIG 3/4.0. Newer SWIG can emit bindings it
  # does not compile against; this is a warning rather than a hard stop because
  # it may well work, but it is the first thing to suspect on a SWIG error.
  local swig_ver
  swig_ver="$(swig -version | awk '/SWIG Version/ {print $3}' || true)"
  case "$swig_ver" in
    4.0.*|3.*) log_info "SWIG $swig_ver" ;;
    *) log_warn "SWIG $swig_ver is newer than GDAL 3.2 expects (3.x / 4.0.x)."
       log_warn "If the Java bindings fail to compile, install swig@4.0 and put it first on PATH." ;;
  esac
}

get_git() {
  local url=$1 dir=$2 tag=$3
  log_info "Fetching $dir at $tag..."
  mkdir -p "$dir"
  pushd "$dir" >/dev/null
  if [ -d .git ]; then git fetch --all --tags; git reset --hard; else git clone "$url" .; fi
  git checkout "$tag"
  popd >/dev/null
}

# --- PROJ --------------------------------------------------------------------

build_proj() {
  log_info "Building PROJ $PROJ_TAG..."
  get_git "$PROJ_GH_URL" proj "$PROJ_TAG"

  pushd proj >/dev/null
  rm -rf build install
  mkdir -p build && cd build

  # PROJ needs SQLite to build proj.db. Unlike GDAL's configure, CMake fails
  # outright on a path that is not there, so this one would at least be loud --
  # resolve it through brew anyway, for the same reason as everywhere else.
  local p_sqlite; p_sqlite="$(require_prefix sqlite)" || exit 1

  # Every app is disabled by name, not by BUILD_APPS. That option does not exist
  # in PROJ 7.2, so CMake accepts and ignores it, leaving each app enabled --
  # and projsync then fails configuration outright with "projsync requires Curl"
  # because ENABLE_CURL is off. Nothing here needs the command-line tools: this
  # build exists to produce libproj for GDAL to link against.
  cmake \
    -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
    -DCMAKE_OSX_ARCHITECTURES="$ARCH" \
    -DCMAKE_INSTALL_PREFIX="$ROOT/proj/install" \
    -DCMAKE_INSTALL_DATADIR="$ROOT/proj/install/share" \
    -DCMAKE_C_FLAGS="-w" -DCMAKE_CXX_FLAGS="-w" \
    -DBUILD_SHARED_LIBS=ON \
    -DBUILD_TESTING=OFF \
    -DENABLE_TIFF=OFF \
    -DENABLE_CURL=OFF \
    -DBUILD_APPS=OFF \
    -DBUILD_CCT=OFF \
    -DBUILD_CS2CS=OFF \
    -DBUILD_GEOD=OFF \
    -DBUILD_GIE=OFF \
    -DBUILD_PROJ=OFF \
    -DBUILD_PROJINFO=OFF \
    -DBUILD_PROJSYNC=OFF \
    -DSQLite3_INCLUDE_DIR="$p_sqlite/include" \
    -DSQLite3_LIBRARY="$p_sqlite/lib/libsqlite3.dylib" \
    ..
  cmake --build . -j "$JOBS"
  cmake --install .
  popd >/dev/null

  PROJ_PREFIX="$ROOT/proj/install"
  log_info "PROJ installed to $PROJ_PREFIX"
}

# --- GDAL --------------------------------------------------------------------

build_gdal() {
  log_info "Building GDAL $GDAL_TAG (autotools -- 3.2 predates the CMake build)..."
  get_git "$GDAL_GH_URL" gdal "$GDAL_TAG"

  # Until GDAL 3.5 the sources live in a gdal/ subdirectory of the repository.
  local src="$ROOT/gdal/gdal"
  [ -d "$src" ] || src="$ROOT/gdal"

  pushd "$src" >/dev/null

  export JAVA_HOME
  export CFLAGS="-w -arch ${ARCH%%;*}"
  export CXXFLAGS="-w -arch ${ARCH%%;*}"

  # netCDF renamed its _FillValue macro to NC_FillValue -- an underscore
  # followed by a capital is reserved in C and C++, so the old spelling was
  # never theirs to define. GDAL 3.2.1 predates the change and uses _FillValue
  # as the attribute name throughout netcdfdataset.cpp and netcdflayer.cpp,
  # which stops compiling against the 4.10 Homebrew now ships: build 11 died
  # with thirteen "use of undeclared identifier" errors and a failed frmts
  # target, immediately after netCDF support was finally switched on.
  #
  # Define it back to the string literal the old header gave it. That is what
  # the macro always expanded to, so this is not a workaround so much as
  # supplying a definition that moved. Spelling it as the literal rather than as
  # NC_FillValue also keeps it working against a netcdf too old to have the new
  # name, and an older header defining it identically produces no diagnostic.
  export CPPFLAGS='-D_FillValue=\"_FillValue\"'

  # Resolve every dependency through brew rather than composing <prefix>/opt/<name>
  # by hand. That assumption is what removed netCDF from build 8: the formula was
  # not installed, /usr/local/opt/netcdf did not exist, and configure treated the
  # path as simply not panning out.
  local p_sqlite p_tiff p_jpeg p_png p_geos p_netcdf
  p_sqlite="$(require_prefix sqlite)"      || exit 1
  p_tiff="$(require_prefix libtiff)"       || exit 1
  p_jpeg="$(require_prefix jpeg-turbo)"    || exit 1
  p_png="$(require_prefix libpng)"         || exit 1
  p_geos="$(require_prefix geos)"          || exit 1
  p_netcdf="$(require_prefix netcdf)"      || exit 1

  # Enable only what is needed and disable the rest. Auto-detection would bind
  # the result to Homebrew paths, and every optional driver enabled here is
  # another chance for 2021 sources to fail against 2026 headers. HDF5, Poppler,
  # OpenJPEG and LibKML are off deliberately, following the reference script,
  # which had to disable them at GDAL 3.9.1 for exactly that reason.
  #
  # GeoTIFF uses GDAL's bundled copy rather than Homebrew's. Homebrew builds
  # libgeotiff against its own PROJ, currently 9, and build 12's libgeotiff.dylib
  # duly referenced libproj.25 while GDAL used the 7.2.1 built here. Two PROJ
  # major versions resolving in one process is not a packaging detail to tidy up
  # afterwards; it is the numeric inconsistency this whole build exists to
  # remove. The internal copy compiles against whatever PROJ GDAL is configured
  # with, so the question cannot arise.
  local config_log="$ROOT/gdal-configure.log"
  ./configure \
    --prefix="$ROOT/gdal/install" \
    --with-proj="$PROJ_PREFIX" \
    --with-sqlite3="$p_sqlite" \
    --with-libtiff="$p_tiff" \
    --with-geotiff=internal \
    --with-jpeg="$p_jpeg" \
    --with-png="$p_png" \
    --with-geos="$p_geos/bin/geos-config" \
    --with-netcdf="$p_netcdf" \
    --with-java="$JAVA_HOME" \
    --with-curl \
    --without-python \
    --without-perl \
    --without-hdf5 \
    --without-poppler \
    --without-libkml \
    --without-openjpeg \
    --without-pg \
    --without-mysql \
    --disable-static \
    --enable-shared 2>&1 | tee "$config_log"
  [ "${PIPESTATUS[0]}" -eq 0 ] || { log_error "configure failed."; exit 1; }

  # configure reports a missing optional dependency in its summary and exits
  # zero, so nothing downstream notices. Build 8 was green, packaged, and had no
  # netCDF driver. Read the summary back and refuse to go on if something asked
  # for above is not in it.
  # The labels and values here are GDAL 3.2's, taken from a real run rather than
  # guessed: the GeoTIFF line is called LIBGEOTIFF, and a dependency is reported
  # as yes, external or internal depending on how it was satisfied. So the test
  # is "not no" -- checking for "yes" would fail the build over LIBGEOTIFF
  # reporting the external copy it was told to use.
  supported() {
    local label="$1" val
    val="$(grep -E "^ *$label support:" "$config_log" | head -1 | sed -E 's/.*support: *//' | tr -d '\r')"
    [ -n "$val" ] && [ "$val" != "no" ]
  }
  local sup missing_support=()
  for sup in NetCDF SQLite GEOS LIBGEOTIFF LIBTIFF LIBPNG LIBJPEG; do
    supported "$sup" || missing_support+=("$sup")
  done
  log_info "configure summary:"
  grep -E "^ *[A-Za-z0-9]+ support: " "$config_log" | sed 's/^/    /'
  if [ "${#missing_support[@]}" -gt 0 ]; then
    log_error "Requested but disabled by configure: ${missing_support[*]}"
    log_error "The bundle would build and package cleanly without these drivers."
    exit 1
  fi

  make -j "$JOBS"
  make install

  # The Java bindings are a separate target and are not built by `make` above.
  # GDAL 3.2's swig/java/java.opt hardcodes JAVA_INCLUDE to
  # $(JAVA_HOME)/include/linux, so on macOS the compile fails with
  # "'jni_md.h' file not found" -- that header lives in include/darwin here.
  # JAVA_HOME itself resolves correctly; only the platform subdirectory is
  # wrong. Overriding on the make command line wins over the value java.opt
  # sets, and leaves the checkout untouched.
  # GDAL 3.2's swig/java/build.xml compiles the wrapper classes at source and
  # target 7, which javac from JDK 21 refuses outright: "Source option 7 is no
  # longer supported. Use 8 or later." Raise it to 8, the lowest still accepted.
  # These are generated JNI wrappers with no version-sensitive code, so the
  # level only has to be one javac will take.
  #
  # Both mechanisms are used because which one applies depends on how build.xml
  # expresses the level, and a wrong guess costs a full rebuild to discover: the
  # edit covers a hardcoded attribute, ANT_ARGS covers a property. Note the edit
  # has to happen here rather than once, since the fetch resets the checkout.
  sed -i '' -E 's/(source|target)="(1\.)?7"/\1="8"/g' swig/java/build.xml
  export ANT_ARGS="-Dant.build.javac.source=8 -Dant.build.javac.target=8"

  log_info "Building Java bindings (swig/java)..."
  pushd swig/java >/dev/null
  make JAVA_INCLUDE="-I$JAVA_HOME/include -I$JAVA_HOME/include/darwin"
  popd >/dev/null

  popd >/dev/null
  GDAL_SRC="$src"
  log_info "GDAL build complete."
}

# --- Naming ------------------------------------------------------------------
#
# The packages Vortex consumes hold one real file per library under an
# unversioned name: gdal-3.5.0_1-macOS-x64.zip, the artifact this replaces,
# ships libgdal.dylib and libhdf5.dylib rather than libgdal.30.dylib with a
# symlink beside it. Reproduce that, because a zip is a poor carrier for
# symlinks -- Gradle's zipTree does not recreate them reliably, and a link
# extracted as a regular file containing the text of its target's name is a
# library that will not load.
#
# The canonical name is the shortest of the names a library answers to in its
# source directory, since libgeos.3.11.0.dylib and the libgeos.dylib symlink
# beside it denote the same file and the reference package uses the latter.
# Deriving it that way rather than by stripping trailing digits is what gets
# libpng16, libxerces-c-3 and libpcre2-8 right, where the number is part of the
# name rather than a version.

# Every name each library answers to, as "<name>=<canonical>" lines, so that a
# versioned dependency reference can be rewritten onto the file actually shipped.
NAME_MAP=""

# Canonical name to the directory the library was copied from, for resolving
# @rpath references, which name a sibling of the original rather than a path.
ORIGIN_MAP=""

# readlink -f is GNU; macOS only grew it in 12.3, and the failure mode is a
# silently empty result. Follow the chain by hand instead.
resolve_link() {
  local p="$1" t
  while [ -L "$p" ]; do
    t="$(readlink "$p")"
    case "$t" in
      /*) p="$t" ;;
      *)  p="$(dirname "$p")/$t" ;;
    esac
  done
  printf '%s' "$p"
}

canonical_name() {
  local file="$1" dir="$2" real best cand n
  real="$(resolve_link "$file")"
  best="$(basename "$real")"
  for cand in "$dir"/*.dylib; do
    # No need to test for a link first: a regular file resolves to itself, so
    # it compares equal only when it is the file already under consideration.
    [ "$(resolve_link "$cand")" = "$real" ] || continue
    n="$(basename "$cand")"
    [ "${#n}" -lt "${#best}" ] && best="$n"
  done
  printf '%s' "$best"
}

# Copy one library into the bundle under its canonical name, dereferencing it,
# and record every name it answers to.
install_lib() {
  local file="$1" dir real canon
  [ -e "$file" ] || return 0
  dir="$(dirname "$file")"
  real="$(resolve_link "$file")"
  [ -f "$real" ] || return 0
  canon="$(canonical_name "$file" "$dir")"
  [ -e "$OUT/gdal/$canon" ] || cp -f "$real" "$OUT/gdal/$canon"
  NAME_MAP="$NAME_MAP
$(basename "$real")=$canon
$(basename "$file")=$canon"
  # Remember where it came from. Once copied, a library has lost the context an
  # @rpath reference needs to be resolved, and its siblings live beside the
  # original.
  ORIGIN_MAP="$ORIGIN_MAP
$canon=$(dirname "$real")"
}

origin_of() {
  printf '%s\n' "$ORIGIN_MAP" | awk -F= -v k="$1" '$1==k {print $2; exit}'
}

map_name() {
  printf '%s\n' "$NAME_MAP" | awk -F= -v k="$1" '$1==k {print $2; exit}'
}

# Pull in the transitive closure of everything already in the bundle. Repeats
# until a pass adds nothing, since each library copied in brings its own
# dependencies. The glob is expanded once per pass, so libraries added during a
# pass are picked up by the next one.
#
# A library already present under its canonical name is never replaced. That is
# what keeps the PROJ built here ahead of Homebrew's, which is a correctness
# matter and not just a preference: they are different major versions.
bundle_dependencies() {
  local changed=1 path dep target deps dep_base dep_origin
  while [ "$changed" -eq 1 ]; do
    changed=0
    for path in "$OUT/gdal/"*.dylib; do
      [ -f "$path" ] || continue
      deps="$(otool -L "$path" 2>/dev/null | tail -n +2 | awk '{print $1}' || true)"
      for dep in $deps; do
        case "$dep" in
          /usr/lib/*|/System/*) continue ;;
          @*)
            # Resolve rather than skip. Skipping assumed an @-prefixed reference
            # named a sibling already in the bundle, and build 13 disproved it:
            # libgeos_c wanted @rpath/libgeos.3.14.1.dylib and libwebp wanted
            # @rpath/libsharpyuv.0.dylib, neither of which was ever copied, and
            # both slipped past verify because it trusted the @ as well. GEOS
            # would not have loaded at all.
            #
            # Homebrew's rpath is the library's own lib directory, so the
            # sibling sits beside the original file -- which is what ORIGIN_MAP
            # remembers, the copy in the bundle having lost that context.
            dep_base="${dep##*/}"
            dep_origin="$(origin_of "$(basename "$path")")"
            if [ -n "$dep_origin" ] && [ -f "$dep_origin/$dep_base" ]; then
              dep="$dep_origin/$dep_base"
            else
              continue
            fi
            ;;
        esac
        [ -f "$dep" ] || continue
        target="$(canonical_name "$dep" "$(dirname "$dep")")"
        # Already here under that name, so leave it. Where the two are genuinely
        # different libraries -- our PROJ 7.2.1 against Homebrew's 9 -- this is
        # the only safe answer. Rewriting the reference to ours instead would
        # link a library compiled against PROJ 9 to PROJ 7, which resolves at
        # load time and misbehaves at run time. Leaving it unmapped means
        # fix_paths cannot place it, verify reports the dangling path by name,
        # and the build fails. That is the outcome to want.
        [ -e "$OUT/gdal/$target" ] && continue
        install_lib "$dep"
        log_info "  + $target (for $(basename "$path"))"
        changed=1
      done
    done
  done
}

# --- Assemble ----------------------------------------------------------------

assemble() {
  log_info "Assembling bundle..."
  rm -rf "$OUT"
  mkdir -p "$OUT/gdal" "$OUT/gdal-data" "$OUT/proj-db"

  # Everything Vortex loads lives flat in gdal/, matching the artifact this
  # replaces: java.library.path points at bin/gdal and the JNI library finds
  # its siblings beside it.
  # Order is precedence: install_lib keeps the first file to claim a canonical
  # name, so what was built here wins over whatever Homebrew has below. That
  # matters most for PROJ, where Homebrew carries 9.x and this bundle has to
  # ship the 7.2.1 that GDAL 3.2 was built against.
  local lib
  for lib in "$ROOT/gdal/install/lib/"*.dylib; do install_lib "$lib"; done
  for lib in "$PROJ_PREFIX/lib/"*.dylib;         do install_lib "$lib"; done

  local jni
  jni="$(find "$GDAL_SRC/swig/java" -name 'libgdalalljni*.dylib' -o -name 'libgdalalljni*.jnilib' | head -1 || true)"
  if [ -z "$jni" ]; then
    log_error "libgdalalljni not found under $GDAL_SRC/swig/java."
    log_error "The Java bindings did not build; the bundle would be useless without them."
    exit 1
  fi
  cp -f "$jni" "$OUT/gdal/libgdalalljni.dylib"
  NAME_MAP="$NAME_MAP
$(basename "$jni")=libgdalalljni.dylib"
  log_info "  JNI: $(basename "$jni") -> libgdalalljni.dylib"

  # Data files ship as their own artifacts (org.gdal:gdal-data, org.proj:proj-db)
  # and must come from these builds, not from the newer ones currently in use:
  # PROJ 7 data and PROJ 9 data are not interchangeable.
  cp -f "$GDAL_SRC/data/"* "$OUT/gdal-data/" 2>/dev/null || true
  cp -f "$PROJ_PREFIX/share/proj/"* "$OUT/proj-db/" 2>/dev/null || true

  # Everything else the bundle loads, worked out by asking the libraries rather
  # than by listing formulae. A hand-maintained list was wrong twice: it missed
  # libnetcdf's entire HDF5 chain, and build 12 shipped 13 libraries where the
  # reference has 54, with libgdal naming Homebrew paths for json-c, webp and
  # gif. A list can only ever be as complete as whoever last edited it; the
  # dependency records are already correct by construction.
  bundle_dependencies
}

# --- Portability -------------------------------------------------------------
#
# Adapted from ras-ui/Build Scripts/GDAL/build-gdal-macos-arm.sh, which is the
# proven part of this script. Every dylib is rewritten to identify itself and
# its siblings through @loader_path, build-directory rpaths are stripped, and
# each library is re-signed -- editing a Mach-O invalidates its signature, and
# macOS will refuse to load an unsigned-but-modified library.

fix_paths() {
  log_info "Rewriting install names to @loader_path..."
  for path in "$OUT/gdal/"*.dylib; do
    [ -f "$path" ] && [ ! -L "$path" ] || continue
    local name; name="$(basename "$path")"
    log_info "  $name"

    install_name_tool -id "@loader_path/$name" "$path" 2>/dev/null || true

    # The `|| true` matters here and below. Under `set -euo pipefail`, a library
    # with no LC_RPATH makes grep exit 1, pipefail propagates that, and the
    # assignment's status kills the script -- with no output at all, since the
    # failing command produced none. An empty result is the normal case for most
    # of these libraries, not an error.
    local rpaths; rpaths="$(otool -l "$path" 2>/dev/null | grep -A2 LC_RPATH | awk '/ path /{print $2}' || true)"
    for rp in $rpaths; do
      case "$rp" in
        @*|/usr/lib*|/System/*) ;;
        *) install_name_tool -delete_rpath "$rp" "$path" 2>/dev/null || true ;;
      esac
    done
    otool -l "$path" 2>/dev/null | grep -A2 LC_RPATH | grep -q "@loader_path" \
      || install_name_tool -add_rpath "@loader_path" "$path" 2>/dev/null || true

    local deps; deps="$(otool -L "$path" 2>/dev/null | tail -n +2 | awk '{print $1}' || true)"
    for dep in $deps; do
      case "$dep" in
        /usr/lib/*|/System/*|@loader_path/*|@executable_path/*) continue ;;
      esac
      # The reference is to whatever name the library had when it was linked,
      # which is usually the versioned one; the bundle holds it under its
      # canonical name, so translate before pointing at it.
      local libname canon; libname="$(basename "$dep")"
      canon="$(map_name "$libname")"
      [ -n "$canon" ] || canon="$libname"
      if [ -e "$OUT/gdal/$canon" ]; then
        install_name_tool -change "$dep" "@loader_path/$canon" "$path" 2>/dev/null || true
      else
        case "$libname" in
          liblzma*|libcurl*|libssl*|libcrypto*|libz.*|libiconv*|libxml2*|libc++*|libSystem*|libresolv*|libexpat*)
            install_name_tool -change "$dep" "/usr/lib/$libname" "$path" 2>/dev/null || true ;;
          *) log_warn "  unhandled dependency in $name: $dep" ;;
        esac
      fi
    done

    codesign --force --sign - "$path" 2>/dev/null || true
  done
}

verify() {
  log_info "Verifying portability..."
  local issues=0
  for path in "$OUT/gdal/"*.dylib; do
    [ -f "$path" ] && [ ! -L "$path" ] || continue
    local bad; bad="$(otool -L "$path" 2>/dev/null | tail -n +2 | awk '{print $1}' \
      | grep -v '^@' | grep -v '^/usr/lib/' | grep -v '^/System/' || true)"
    if [ -n "$bad" ]; then
      log_warn "$(basename "$path") still references:"; echo "$bad" | sed 's/^/      /'
      issues=1
    fi

    # An @-prefixed reference is not self-evidently satisfied: it has to name
    # something that is actually here. Treating the @ as sufficient is what let
    # build 13 report a self-contained bundle while libgeos_c pointed at an
    # @rpath/libgeos that had never been copied.
    local ref refs
    refs="$(otool -L "$path" 2>/dev/null | tail -n +2 | awk '{print $1}' | grep '^@' || true)"
    for ref in $refs; do
      [ -e "$OUT/gdal/${ref##*/}" ] && continue
      log_warn "$(basename "$path") -> $ref, which is not in the bundle"
      issues=1
    done
  done
  # Structure has to match the artifact being replaced: one real file per
  # library, unversioned, no symlinks. A link matters more than it looks --
  # zipTree does not recreate them, so it would arrive as a regular file
  # holding the text of its target's name.
  local links; links="$(find "$OUT/gdal" -type l 2>/dev/null || true)"
  if [ -n "$links" ]; then
    log_warn "symlinks left in gdal/; zipTree will not carry them:"
    echo "$links" | sed 's#.*/#      #'
    issues=1
  fi
  local versioned; versioned="$(find "$OUT/gdal" -name '*.dylib' 2>/dev/null \
    | sed 's#.*/##' | grep -E '\.[0-9][0-9.]*\.dylib$' || true)"
  if [ -n "$versioned" ]; then
    log_warn "versioned names in gdal/; the reference package has none, so these"
    log_warn "libraries had no unversioned name to derive a canonical one from:"
    echo "$versioned" | sed 's/^/      /'
  fi

  [ -e "$OUT/gdal/libgdalalljni.dylib" ] || { log_error "libgdalalljni.dylib missing"; issues=1; }
  [ -e "$OUT/proj-db/proj.db" ] || log_warn "proj.db missing from proj-db/"
  # 3.2.1 is 2021 code against a current Homebrew, and netCDF is the dependency
  # most likely to have been configured out without stopping the build. The
  # artifact being replaced ships one, and Vortex reads .nc files, so its
  # absence is a broken bundle that would otherwise only surface at import.
  [ -e "$OUT/gdal/libnetcdf.dylib" ] || log_warn "libnetcdf.dylib missing; check the configure summary for NetCDF support"
  log_info "gdal/ holds $(find "$OUT/gdal" -name '*.dylib' | wc -l | tr -d ' ') libraries (reference 3.5.0_1 ships 54)."
  if [ "$issues" -eq 0 ]; then
    log_info "Bundle is self-contained."
  else
    log_error "Bundle is NOT self-contained; see above. Refusing to package."
    log_error "A library still naming a Homebrew path resolves on this agent and"
    log_error "nowhere else, so the zip would look correct and fail on use. That"
    log_error "is the failure this build keeps producing, and it is worth a red"
    log_error "build rather than an artifact somebody has to find out about."
    exit 1
  fi
}

package() {
  # Nexus spells the macOS classifier x64, not x86_64: the artifact being
  # replaced is org.gdal:gdal:3.5.0_1:macOS-x64. Naming the zip to match means
  # it can be uploaded as-is, since Maven derives the classifier from the
  # filename.
  local arch_tag
  case "$ARCH" in
    x86_64)        arch_tag="x64" ;;
    arm64|aarch64) arch_tag="arm64" ;;
    *)             arch_tag="${ARCH//;/-}" ;;
  esac
  local ver="${GDAL_TAG#v}"
  log_info "Packaging..."
  ( cd "$OUT" && zip -qry "gdal-$ver-macOS-$arch_tag.zip" gdal )
  ( cd "$OUT" && zip -qry "gdal-data-$ver.zip" gdal-data )
  ( cd "$OUT" && zip -qry "proj-db-$PROJ_TAG.zip" proj-db )
  echo
  log_info "Artifacts in $OUT:"
  ls -1sh "$OUT"/*.zip | sed 's/^/    /'
  echo
  cat <<EOS
Publish to the maven-releases repository; maven-public is a group and cannot be
written to. The coordinates are:

    org.gdal:gdal:$ver         classifier macOS-$arch_tag   zip
    org.gdal:gdal-data:$ver    no classifier        zip
    org.proj:proj-db:$PROJ_TAG    no classifier        zip

landing at these paths, which are what the zip filenames above already say:

    org/gdal/gdal/$ver/gdal-$ver-macOS-$arch_tag.zip
    org/gdal/gdal-data/$ver/gdal-data-$ver.zip
    org/proj/proj-db/$PROJ_TAG/proj-db-$PROJ_TAG.zip

gdal joins the existing $ver, which already carries the linux classifier, so it
needs no new POM. The other two are new versions and do; the Nexus upload form
generates one from the GAV fields. Do not put the platform in the version, as
the Windows artifact does with 3.2.1-win-x64 -- macOS and Linux both use a
classifier, which is what lets one version hold every platform.

Then point the macOS natives at them in build.gradle.kts:

    macOS_x64("org.gdal:gdal:$ver:macOS-$arch_tag@zip")
    macOS_x64("org.gdal:gdal-data:$ver@zip")
    macOS_x64("org.proj:proj-db:$PROJ_TAG@zip")

and set the macOS Java binding in vortex-api and vortex-ui to org.gdal:gdal:3.2.0
so the binding matches the native, as Windows and Linux already do.
EOS
}

main() {
  log_info "GDAL $GDAL_TAG + PROJ $PROJ_TAG for macOS ($ARCH)"
  log_info "Build root: $ROOT"
  preflight
  install_deps
  build_proj
  build_gdal
  assemble
  fix_paths
  verify
  package
}

main "$@"
