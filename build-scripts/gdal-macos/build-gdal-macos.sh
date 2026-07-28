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
# THIS SCRIPT HAS NOT BEEN RUN. Read this before trusting it.
# -----------------------------------------------------------------------------
# It is assembled from a working reference -- ras-ui's build-gdal-macos-arm.sh,
# whose @loader_path rewriting is proven -- plus the requirements of GDAL 3.2's
# own build system. Nobody has executed it, because no macOS machine was
# available. Three things in particular are likely to need attention:
#
#   1. GDAL 3.2 predates GDAL's CMake build, which only became primary in 3.6.
#      This uses ./configure, so the reference script's cmake invocation does
#      not carry over and the flag names below are from GDAL 3.2's configure,
#      not from a build anyone has watched succeed.
#
#   2. Java bindings on macOS are the known-fragile part. See
#      https://github.com/OSGeo/gdal/issues/10316 -- they fail to build from
#      source on macOS for GDAL 3.7 and newer. 3.2 predates that report, which
#      means the specific breakage does not apply, not that it builds cleanly.
#
#   3. 3.2.1 is 2021 code being compiled against 2026 Homebrew libraries. The
#      reference script had to disable NETCDF, POPPLER, LIBKML, PARQUET, HDF5
#      and OPENJPEG at GDAL 3.9.1 for exactly this reason, and 3.2 is four
#      years further back. Expect to disable more, or to pin older dependencies.
#      NETCDF matters here: the artifact this replaces ships libnetcdf.dylib.
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

brew_prefix() { [ -d /opt/homebrew ] && echo /opt/homebrew || echo /usr/local; }

# --- Preflight ---------------------------------------------------------------

preflight() {
  log_info "Checking prerequisites..."

  # CMake and configure both mishandle install prefixes containing spaces.
  case "$ROOT" in
    *" "*) log_error "Build path contains spaces: $ROOT"; exit 1 ;;
  esac

  xcode-select -p &>/dev/null || {
    log_error "Xcode Command Line Tools missing: xcode-select --install"; exit 1; }
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

  # One at a time rather than one invocation with every name: brew aborts the
  # whole batch on a name it cannot resolve, so a single bad formula would
  # silently leave everything after it uninstalled.
  local pkg
  log_info "Installing build tools..."
  for pkg in autoconf automake libtool pkg-config swig ant; do
    if brew install --formula --quiet "$pkg" >/dev/null 2>&1; then
      log_info "  $pkg"
    else
      log_warn "  $pkg -- brew install failed (may already be present)"
    fi
  done

  log_info "Installing libraries..."
  for pkg in sqlite libtiff libgeotiff jpeg-turbo libpng geos zstd netcdf; do
    if brew install --formula --quiet "$pkg" >/dev/null 2>&1; then
      log_info "  $pkg"
    else
      log_warn "  $pkg -- brew install failed (may already be present)"
    fi
  done

  # Verify afterwards rather than before. A failed brew install above is not
  # itself fatal -- it exits non-zero for benign reasons such as the formula
  # already being installed -- so the check that matters is whether the tool is
  # on PATH now.
  local missing=()
  for tool in swig ant autoconf automake libtool pkg-config; do
    command -v "$tool" &>/dev/null || missing+=("$tool")
  done
  if [ "${#missing[@]}" -gt 0 ]; then
    log_error "Still missing after brew install: ${missing[*]}"
    log_error "Install them on this machine and re-run."
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

  local bp; bp="$(brew_prefix)"

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
    -DSQLite3_INCLUDE_DIR="$bp/opt/sqlite/include" \
    -DSQLite3_LIBRARY="$bp/opt/sqlite/lib/libsqlite3.dylib" \
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

  local bp; bp="$(brew_prefix)"
  export JAVA_HOME
  export CFLAGS="-w -arch ${ARCH%%;*}"
  export CXXFLAGS="-w -arch ${ARCH%%;*}"

  # Enable only what is needed and disable the rest. Auto-detection would bind
  # the result to /opt/homebrew paths, and every optional driver enabled here
  # is another chance for 2021 sources to fail against 2026 headers.
  ./configure \
    --prefix="$ROOT/gdal/install" \
    --with-proj="$PROJ_PREFIX" \
    --with-sqlite3="$bp/opt/sqlite" \
    --with-libtiff="$bp/opt/libtiff" \
    --with-geotiff="$bp/opt/libgeotiff" \
    --with-jpeg="$bp/opt/jpeg-turbo" \
    --with-png="$bp/opt/libpng" \
    --with-geos="$bp/opt/geos/bin/geos-config" \
    --with-netcdf="$bp/opt/netcdf" \
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
    --enable-shared

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

# --- Assemble ----------------------------------------------------------------

assemble() {
  log_info "Assembling bundle..."
  rm -rf "$OUT"
  mkdir -p "$OUT/gdal" "$OUT/gdal-data" "$OUT/proj-db"

  # Everything Vortex loads lives flat in gdal/, matching the artifact this
  # replaces: java.library.path points at bin/gdal and the JNI library finds
  # its siblings beside it.
  cp -Pf "$ROOT/gdal/install/lib/"*.dylib "$OUT/gdal/" 2>/dev/null || true
  cp -Pf "$PROJ_PREFIX/lib/"*.dylib "$OUT/gdal/" 2>/dev/null || true

  local jni
  jni="$(find "$GDAL_SRC/swig/java" -name 'libgdalalljni*.dylib' -o -name 'libgdalalljni*.jnilib' | head -1 || true)"
  if [ -z "$jni" ]; then
    log_error "libgdalalljni not found under $GDAL_SRC/swig/java."
    log_error "The Java bindings did not build; the bundle would be useless without them."
    exit 1
  fi
  cp -f "$jni" "$OUT/gdal/libgdalalljni.dylib"
  log_info "  JNI: $(basename "$jni")"

  # Data files ship as their own artifacts (org.gdal:gdal-data, org.proj:proj-db)
  # and must come from these builds, not from the newer ones currently in use:
  # PROJ 7 data and PROJ 9 data are not interchangeable.
  cp -f "$GDAL_SRC/data/"* "$OUT/gdal-data/" 2>/dev/null || true
  cp -f "$PROJ_PREFIX/share/proj/"* "$OUT/proj-db/" 2>/dev/null || true

  # Homebrew dependencies, copying real files once and recreating symlinks as
  # relative so a chain like libfoo.dylib -> libfoo.1.dylib is not duplicated.
  local bp; bp="$(brew_prefix)"
  copy_brew_lib() {
    local dir="$1" pat="$2"
    [ -d "$dir" ] || return 0
    for lib in "$dir/"$pat; do
      [ -f "$lib" ] && [ ! -L "$lib" ] && cp -f "$lib" "$OUT/gdal/$(basename "$lib")" || true
    done
    for lib in "$dir/"$pat; do
      if [ -L "$lib" ]; then
        local name target; name="$(basename "$lib")"; target="$(basename "$(readlink -f "$lib")")"
        [ -e "$OUT/gdal/$target" ] && [ ! -e "$OUT/gdal/$name" ] && ln -sf "$target" "$OUT/gdal/$name" || true
      fi
    done
  }
  copy_brew_lib "$bp/opt/sqlite/lib"      "libsqlite3*.dylib"
  copy_brew_lib "$bp/opt/libtiff/lib"     "libtiff*.dylib"
  copy_brew_lib "$bp/opt/libgeotiff/lib"  "libgeotiff*.dylib"
  copy_brew_lib "$bp/opt/jpeg-turbo/lib"  "libjpeg*.dylib"
  copy_brew_lib "$bp/opt/libpng/lib"      "libpng*.dylib"
  copy_brew_lib "$bp/opt/geos/lib"        "libgeos*.dylib"
  copy_brew_lib "$bp/opt/zstd/lib"        "libzstd*.dylib"
  copy_brew_lib "$bp/opt/netcdf/lib"      "libnetcdf*.dylib"
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
      local libname; libname="$(basename "$dep")"
      if [ -e "$OUT/gdal/$libname" ]; then
        install_name_tool -change "$dep" "@loader_path/$libname" "$path" 2>/dev/null || true
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
  done
  [ -e "$OUT/gdal/libgdalalljni.dylib" ] || { log_error "libgdalalljni.dylib missing"; issues=1; }
  [ -e "$OUT/proj-db/proj.db" ] || log_warn "proj.db missing from proj-db/"
  [ "$issues" -eq 0 ] && log_info "Bundle is self-contained." || log_warn "Bundle is NOT self-contained; see above."
}

package() {
  local arch_tag="${ARCH//;/-}"
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
To use these in Vortex, publish them to the HEC Nexus and point the macOS
natives at them in build.gradle.kts:

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
