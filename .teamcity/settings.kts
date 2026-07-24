import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
 * TeamCity project configuration for Vortex.
 *
 * Vortex is a Gradle multi-project build that produces a self-contained,
 * per-platform distribution (bundled JRE + GDAL/netCDF/HDF/heclib natives).
 * Because the native libraries and the JRE differ per operating system, and
 * because the `getNatives`, `build`, and `zip` tasks branch on the *running*
 * OS (see build.gradle.kts), there is no cross-compilation: each platform is
 * built on an agent running that platform.
 *
 * Versioning is handled by the `nebula.release` plugin, which derives the
 * semantic version from git tags. There is no version in a file — the tags
 * ARE the source of truth. This shapes the pipeline into three roles:
 *
 *   1. CI builds (Build — Linux/Windows/macOS): build + test every branch
 *      commit. nebula derives a dev version (e.g. 1.2.1-dev.3+abc1234).
 *
 *   2. Release (nebula): manually triggered. Runs `final`/`candidate`, which
 *      computes the next semantic version and CREATES + PUSHES a git tag
 *      (e.g. v1.2.1). It publishes the vortex/vortex-ui libraries to Nexus
 *      ONLY for `final`/`candidate` (never `snapshot`). This is the only place
 *      a tag is minted, so it runs on ONE agent.
 *
 *   3. Package builds (Package — Linux/Windows/macOS): triggered by the tag
 *      that Release pushes. Each checks out that tag and runs the build with
 *      -Prelease.useLastTag=true, so nebula reuses the existing tag's version
 *      WITHOUT minting another one. This is the fan-out that produces the
 *      release-versioned installers for all three operating systems.
 *
 * Minting the version (once) and building the installers (per OS) are split
 * precisely because a tag must be created exactly once while installers must
 * be built on three different machines.
 *
 * This is the portable ("versioned settings") Kotlin DSL. TeamCity also needs
 * the companion .teamcity/pom.xml (generated automatically by the server, or
 * created when editing this DSL in an IDE).
 */

version = "2026.1"

project {
    description = "Vortex — build, test, and package for Windows, macOS, and Linux"

    params {
        // Git credentials. nebula must PUSH the release tag, so a token with
        // write access is required (not just read). Define real values as
        // secure project parameters in the TeamCity UI.
        param("github.user", "")
        password("github.token", "")
        // Nexus publishing credentials, read by the Gradle publishing block via
        // the mavenUser / mavenPassword project properties.
        param("mavenUser", "")
        password("mavenPassword", "")
    }

    // Shared VCS root and build template live on the umbrella project so both
    // sub-projects inherit them (as do the project parameters above).
    vcsRoot(VortexVcs)
    template(PlatformBuild)

    // Release lives in its own sub-project so the "Run build" permission can be
    // granted to release managers ALONE. TeamCity has no per-build-config
    // permission — a project is the finest boundary — and permissions inherit
    // downward and are additive, so the umbrella project must NOT grant "run"
    // broadly. Recommended role assignments (set in the TeamCity UI, not here):
    //   - Vortex (this project):     everyone -> Project Viewer
    //   - Vortex / CI:               developers -> a role that includes Run build
    //   - Vortex / Release:          release managers only -> Run build
    // Also restrict who can push v* tags in GitHub: the Package fan-out triggers
    // on any pushed tag, so tag-push rights are a second lock on releasing.
    subProject(CiProject)
    subProject(ReleaseProject)
}

/**
 * CI sub-project: the continuous per-OS builds plus the tag-triggered Package
 * fan-out. Grant developers a run-capable role here.
 */
object CiProject : Project({
    id("Vortex_CI")
    name = "CI"
    description = "Per-OS build + test, and the tag-triggered release-installer fan-out"

    // CI: build + test every branch commit.
    buildType(BuildLinux)
    buildType(BuildWindows)
    buildType(BuildMacOS)

    // Fan-out: build release installers for each OS from the pushed tag.
    buildType(PackageLinux)
    buildType(PackageWindows)
    buildType(PackageMacOS)

    buildTypesOrder = listOf(
        BuildLinux, BuildWindows, BuildMacOS,
        PackageLinux, PackageWindows, PackageMacOS
    )
})

/**
 * Release sub-project: the single manually triggered build that mints the tag.
 * Grant the "Run build" permission here to release managers only — that is what
 * makes this build the sole controlled entry point for cutting a release.
 */
object ReleaseProject : Project({
    id("Vortex_Release")
    name = "Release"
    description = "Restricted: mints + publishes a semantic version"

    buildType(Release)
})

/**
 * Git VCS root that treats nebula's release tags as branches.
 *
 * `branchSpec` maps every refs/tags/v* ref to a logical branch named after the
 * tag (the capture group), which is what lets a tag push trigger builds and
 * lets the agent check the tag out onto HEAD (so nebula's useLastTag can read
 * it). The default branch stays master for ordinary CI.
 */
object VortexVcs : GitVcsRoot({
    id("VortexVcs")
    name = "Vortex"
    url = "https://github.com/HydrologicEngineeringCenter/Vortex.git"
    pushUrl = "https://github.com/HydrologicEngineeringCenter/Vortex.git"
    branch = "refs/heads/master"
    // Also monitor release + rc tags (v1.2.3, v1.2.3-rc.1) as logical branches.
    // Including refs/tags in the branch spec is what makes a tag push trigger
    // builds and lets the agent check the tag out onto HEAD (so nebula's
    // useLastTag can read it). The (v*) capture names the logical branch.
    branchSpec = "+:refs/tags/(v*)"
    userNameStyle = GitVcsRoot.UserNameStyle.NAME
    authMethod = password {
        userName = "%github.user%"
        password = "%github.token%"
    }
})

/**
 * Shared build logic for a single platform: extract the OS-specific native
 * libraries, run the test suite against them, then assemble the distribution
 * archive. Concrete per-OS configurations below add an agent requirement and a
 * trigger. The `gradle.extraGradleParams` parameter lets the Package builds
 * inject -Prelease.useLastTag=true without duplicating the steps.
 */
object PlatformBuild : Template({
    id("PlatformBuild")
    name = "Platform Build"

    // Per-OS build.gradle.kts produces exactly one of these into build/distributions:
    //   vortex-<version>-win-x64.zip / -linux-x64.tar.gz / -macOS-x64.zip
    artifactRules = """
        build/distributions/vortex-*-*.zip
        build/distributions/vortex-*-*.tar.gz
    """.trimIndent()

    params {
        // Empty for CI (dev version). Package builds override this with
        // -Prelease.useLastTag=true so the installers carry the tag's version.
        param("gradle.extraGradleParams", "")
    }

    vcs {
        root(VortexVcs)
        // nebula.release reads git tags/history to infer the version, so the
        // checkout must happen on the agent with the full .git directory.
        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        // AGENTS.md: getNatives must run *before* tests, or every NetCDF-backed
        // test fails with UnsatisfiedLinkError. `clean` first for a fresh build.
        gradle {
            name = "Extract native libraries (getNatives)"
            tasks = "clean getNatives"
            useGradleWrapper = true
            gradleWrapperPath = ""
            jdkHome = "%env.JDK_21_0_x64%"
        }
        // Run tests explicitly against the extracted natives. vortex-api sets
        // ignoreFailures = true, so Gradle exits 0 even on failures; TeamCity's
        // own test reporting (below) is what actually fails the build.
        gradle {
            name = "Test"
            tasks = "test"
            useGradleWrapper = true
            gradleWrapperPath = ""
            jdkHome = "%env.JDK_21_0_x64%"
        }
        // Assemble the distribution (copies JRE, natives, UI, license, scripts,
        // fat jar) and zip it. `-x test` avoids re-running the suite from step 2.
        // extraGradleParams carries -Prelease.useLastTag=true on Package builds.
        gradle {
            name = "Assemble distribution"
            tasks = "build -x test"
            gradleParams = "%gradle.extraGradleParams%"
            useGradleWrapper = true
            gradleWrapperPath = ""
            jdkHome = "%env.JDK_21_0_x64%"
        }
    }

    features {
        perfmon {}
    }

    // Gradle needs JDK 21 (the bundled runtime and the Adoptium JRE dependencies
    // are 21.0.9); source/target is 17. Not every agent provides one, and some
    // provide only an ARM64 build, so requiring the x64 capability — rather than
    // just an OS — is what routes each build to an agent that can actually run
    // it. The concrete per-OS configurations below add their own OS requirement
    // on top of this.
    requirements {
        exists("env.JDK_21_0_x64")
    }

    failureConditions {
        executionTimeoutMin = 60
        // Default TeamCity behavior fails the build if any test fails — this is
        // what catches failures that Gradle swallows via ignoreFailures = true.
        testFailure = true
    }
})

// ----------------------------------------------------------------------------
// CI builds — one per OS, triggered on branch commits (tags excluded).
// ----------------------------------------------------------------------------

// Branch filter shared by the CI builds: build any branch, but NOT the tag
// "branches" (those are handled by the Package builds below).
const val CI_BRANCH_FILTER = """
    +:*
    -:v*
"""

object BuildLinux : BuildType({
    templates(PlatformBuild)
    id("Build_Linux")
    name = "Build — Linux (x64)"

    triggers { vcs { branchFilter = CI_BRANCH_FILTER.trimIndent() } }
    requirements { contains("teamcity.agent.jvm.os.name", "Linux") }
})

object BuildWindows : BuildType({
    templates(PlatformBuild)
    id("Build_Windows")
    name = "Build — Windows (x64)"

    triggers { vcs { branchFilter = CI_BRANCH_FILTER.trimIndent() } }
    requirements { contains("teamcity.agent.jvm.os.name", "Windows") }
})

object BuildMacOS : BuildType({
    templates(PlatformBuild)
    id("Build_macOS")
    name = "Build — macOS (x64)"

    triggers { vcs { branchFilter = CI_BRANCH_FILTER.trimIndent() } }
    requirements { contains("teamcity.agent.jvm.os.name", "Mac OS X") }
})

// ----------------------------------------------------------------------------
// Release — mints the semantic version. Runs on ONE agent.
// ----------------------------------------------------------------------------

/**
 * Manually triggered release. Runs a nebula.release task to compute the next
 * semantic version, create + push the git tag, and publish the vortex /
 * vortex-ui libraries to Nexus.
 *
 * `release.task`:
 *   - final     : a production release (e.g. 1.2.0 -> tag v1.2.0)
 *   - candidate : a release candidate (e.g. 1.2.0-rc.1 -> tag v1.2.0-rc.1)
 *   - snapshot  : a snapshot publish (no tag)
 *
 * The tag this pushes is what triggers the Package fan-out below. This build
 * itself does NOT assemble installers: `-x build` skips the OS-specific
 * distribution (which cannot be cross-built anyway). Nexus publishing is a
 * separate, conditional step that runs ONLY for `final` and `candidate` — not
 * for `snapshot`. The per-OS installers come from the Package builds.
 */
object Release : BuildType({
    id("Release")
    name = "Release (nebula)"
    description = "Mint the semantic version; pushes the tag that drives the Package fan-out"

    params {
        select(
            "release.task", "final",
            label = "Release task",
            description = "nebula.release task to run",
            options = listOf("final", "candidate", "snapshot")
        )
        // Optional extra args, e.g. -Prelease.scope=minor to force a minor bump.
        param("release.gradleParams", "")
    }

    vcs {
        root(VortexVcs)
        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        // Mint + push the tag only. Publishing is excluded here so that it is
        // driven entirely by the conditional step below (final/candidate only);
        // -x build also skips the OS-specific distribution.
        gradle {
            name = "Release / tag (%release.task%)"
            tasks = "%release.task%"
            gradleParams = "-x build -x :vortex-api:publish -x :vortex-ui:publish %release.gradleParams%"
            useGradleWrapper = true
            gradleWrapperPath = ""
            jdkHome = "%env.JDK_21_0_x64%"
        }
        // Publish the libraries to Nexus for tagged releases only. The step
        // condition keeps snapshots off Nexus. useLastTag pins the version to
        // the tag the previous step just created.
        gradle {
            name = "Publish libraries to Nexus"
            tasks = ":vortex-api:publish :vortex-ui:publish"
            gradleParams =
                "-Prelease.useLastTag=true -PmavenUser=%mavenUser% -PmavenPassword=%mavenPassword%"
            useGradleWrapper = true
            gradleWrapperPath = ""
            jdkHome = "%env.JDK_21_0_x64%"
            conditions {
                // Runs for release.task == final OR candidate; skipped for snapshot.
                matches("release.task", "final|candidate")
            }
        }
    }

    failureConditions {
        executionTimeoutMin = 60
    }

    // Any agent with a JDK 21 can mint the tag + publish; pin to Linux for
    // determinism. Only some Linux agents carry a JDK 21, so the capability
    // requirement is what keeps this off the ones that do not.
    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
        exists("env.JDK_21_0_x64")
    }
})

// ----------------------------------------------------------------------------
// Package fan-out — one per OS, triggered by the tag Release pushes.
// ----------------------------------------------------------------------------

// Trigger filter for the Package builds: only the tag "branches" (v1.2.3,
// v1.2.3-rc.1, ...). Each new release/candidate tag fans out to all three.
const val TAG_BRANCH_FILTER = "+:v*"

// -Prelease.useLastTag=true tells nebula to reuse the tag already on HEAD
// instead of computing/creating a new one, so the installer version matches
// the release exactly.
const val USE_LAST_TAG = "-Prelease.useLastTag=true"

object PackageLinux : BuildType({
    templates(PlatformBuild)
    id("Package_Linux")
    name = "Package — Linux (x64)"
    description = "Build the Linux release installer from the pushed tag"

    params { param("gradle.extraGradleParams", USE_LAST_TAG) }
    triggers { vcs { branchFilter = TAG_BRANCH_FILTER } }
    requirements { contains("teamcity.agent.jvm.os.name", "Linux") }
})

object PackageWindows : BuildType({
    templates(PlatformBuild)
    id("Package_Windows")
    name = "Package — Windows (x64)"
    description = "Build the Windows release installer from the pushed tag"

    params { param("gradle.extraGradleParams", USE_LAST_TAG) }
    triggers { vcs { branchFilter = TAG_BRANCH_FILTER } }
    requirements { contains("teamcity.agent.jvm.os.name", "Windows") }
})

object PackageMacOS : BuildType({
    templates(PlatformBuild)
    id("Package_macOS")
    name = "Package — macOS (x64)"
    description = "Build the macOS release installer from the pushed tag"

    params { param("gradle.extraGradleParams", USE_LAST_TAG) }
    triggers { vcs { branchFilter = TAG_BRANCH_FILTER } }
    requirements { contains("teamcity.agent.jvm.os.name", "Mac OS X") }
})
