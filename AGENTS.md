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
- `NetcdfDataWriterTest.IntervalTimeCircleTest` has a pre-existing,
  environmental failure (path-separator and timezone rendering differences).
  It fails independently of local changes — do not treat it as a regression.

## Git

- **Root-level files are git-ignored by default.** `.gitignore` starts with
  `/*`, then re-allows specific paths with `!` entries. Any new file at the
  repository root (e.g. a doc) needs its own `!<filename>` allowlist entry in
  `.gitignore`, or git will silently ignore it.
