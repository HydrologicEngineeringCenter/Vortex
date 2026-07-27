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

## TeamCity configuration

- The pipeline lives in `.teamcity/settings.kts` (portable Kotlin DSL). The
  server compiles it by running Maven against `.teamcity/pom.xml`, so that pom
  must declare the DSL dependencies — it is not just IDE scaffolding.
- **Validate locally before pushing.** The server only reports compile errors
  after a commit, so verifying on the server means committing a patch per
  error. Instead run:

      mvn -f .teamcity/pom.xml teamcity-configs:generate

  Success writes the generated XML under `.teamcity/target/generated-configs`
  (git-ignored). This is exactly what the server does, so a clean run here
  means a clean import there.
- The pom depends on `configs-dsl-kotlin-latest`, not `configs-dsl-kotlin`.
  The latter ships the versioned `...configs.kotlin.v2019_2` API; `settings.kts`
  uses the modern unversioned `jetbrains.buildServer.configs.kotlin` package,
  and mixing them fails with "unresolved supertypes" on every DSL class.
- `settings.kts` is a *script*, not a regular Kotlin file. Two consequences:
  top-level `const val` is rejected outright, and a plain top-level `val`
  referenced from an `object ... : BuildType({ ... })` fails with "captures the
  script class instance". Put shared literals as `const val` inside a named
  object (see `Config`), which inlines them at each use site.

## Git

- **Root-level files are git-ignored by default.** `.gitignore` starts with
  `/*`, then re-allows specific paths with `!` entries. Any new file at the
  repository root (e.g. a doc) needs its own `!<filename>` allowlist entry in
  `.gitignore`, or git will silently ignore it.
