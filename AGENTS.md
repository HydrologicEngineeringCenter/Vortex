# AGENTS.md

Project-specific notes for working in this repository.

## Environment

- Platform: Windows. Primary shell is PowerShell. Build with `./gradlew`
  (`gradlew.bat` on Windows).

## Building and testing

- **Run `./gradlew getNatives` before running tests.** It extracts the GDAL,
  netCDF, and HDF native libraries into `bin/`. Without it, every NetCDF-backed
  test fails with `java.lang.UnsatisfiedLinkError` (e.g. GDAL `osrJNI`) because
  the native libraries are not on the path.
- **`BUILD SUCCESSFUL` does not mean tests passed.** The `vortex-api` test task
  sets `ignoreFailures = true`, so Gradle reports success even when tests fail.
  Confirm results in `vortex-api/build/test-results/test/*.xml` (or the HTML
  report under `vortex-api/build/reports/tests/`).
- Tests run with `-Djava.io.tmpdir=C:/Temp` on Windows.
- `VortexGrid.equals`/`hashCode` compare the *resolved* data type, `dataType()`,
  not the raw field. The getter infers a type for grids built as `UNDEFINED`
  (non-zero interval + a known variable name), and the NetCDF writer persists
  that inferred value as `cell_methods`, so a grid written as `UNDEFINED` reads
  back with a concrete type. Comparing the raw field made those two unequal and
  broke `NetcdfDataWriterTest.IntervalTimeCircleTest`. Note that the `toString`
  of two equal grids can still differ in `fileName` separators and time-zone
  rendering — neither is compared, so don't read a diff of them as the cause.

## Git

- **Root-level files are git-ignored by default.** `.gitignore` starts with
  `/*`, then re-allows specific paths with `!` entries. Any new file at the
  repository root (e.g. a doc) needs its own `!<filename>` allowlist entry in
  `.gitignore`, or git will silently ignore it.
