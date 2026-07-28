import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.GradleBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.failureConditions.BuildFailureOnText
import jetbrains.buildServer.configs.kotlin.failureConditions.failOnText
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
 *   1. CI builds (Build — Linux/Windows/macOS): build + test a branch.
 *      nebula derives a dev version (e.g. 1.2.1-dev.3+abc1234).
 *
 *   2. Release (nebula): runs `final`/`candidate`, which computes the next
 *      semantic version and CREATES + PUSHES a git tag (e.g. v1.2.1). It
 *      publishes the vortex/vortex-ui libraries to Nexus ONLY for `final`; a
 *      candidate is tagged and packaged but not published. This is the only
 *      place a tag is minted, so it runs on ONE agent.
 *
 *   3. Package builds (Package — Linux/Windows/macOS): triggered by the tag that
 *      Release pushes. The VCS root's branchSpec exposes every v* tag as a
 *      logical branch, which is what lets the tag push start these. Each checks
 *      that tag out and builds with -Prelease.useLastTag=true, so nebula reuses
 *      the existing tag's version WITHOUT minting another one. This is the
 *      fan-out producing release-versioned installers for all three systems.
 *
 * Minting the version (once) and building the installers (per OS) are split
 * precisely because a tag must be created exactly once while installers must
 * be built on three different machines.
 *
 * THERE IS EXACTLY ONE TRIGGER, on the Package builds, filtered to +:v* — that
 * is, to pushed release tags and nothing else. Ordinary commits start nothing:
 * the CI builds and Release are run by hand. That asymmetry is deliberate.
 * TeamCity's "Run build" permission governs only manual starts, since triggers
 * fire as the system with no permission check, so the set of triggers — not the
 * permission model — is what decides when the shared agents get used. Keeping
 * the only trigger on a ref that just one build can create (Release, in its own
 * permission-scoped sub-project) means the fan-out is automatic while still
 * requiring a person to have started the release. Adding a `triggers` block
 * anywhere else, especially one matching ordinary branches, reverses that.
 *
 * This is the portable ("versioned settings") Kotlin DSL. TeamCity also needs
 * the companion .teamcity/pom.xml (generated automatically by the server, or
 * created when editing this DSL in an IDE).
 */

version = "2026.1"

project {
    description = "Vortex — build, test, and package for Windows, macOS, and Linux"

    params {
        // Git credentials. nebula must PUSH the release tag, so the token needs
        // write access, not just read.
        //
        // The credentialsJSON references below are pointers, not secrets: the
        // actual values live on the TeamCity server (the "store secure values
        // outside of VCS" option) and are resolved at build time. This is what
        // keeps real credentials out of this repository, which is public.
        param("github.user", "tombrauer")
        password("github.token", "credentialsJSON:995479fa-5468-431d-9655-39fb22b6484d")
        // Nexus publishing credentials, read by the Gradle publishing block via
        // the mavenUser / mavenPassword project properties.
        param("mavenUser", "hmsupload")
        password("mavenPassword", "credentialsJSON:988750dd-5c51-4a3c-bedb-b75f33b74c56")
    }

    // Shared VCS root and build template live on the umbrella project so both
    // sub-projects inherit them (as do the project parameters above).
    vcsRoot(VortexVcs)
    template(PlatformBuild)
    template(PlatformBuildLinux)

    // Release lives in its own sub-project so the "Run build" permission can be
    // granted to release managers ALONE. TeamCity has no per-build-config
    // permission — a project is the finest boundary — and permissions inherit
    // downward and are additive, so the umbrella project must NOT grant "run"
    // broadly. Recommended role assignments (set in the TeamCity UI, not here):
    //   - Vortex (this project):     everyone -> Project Viewer
    //   - Vortex / CI:               developers -> a role that includes Run build
    //   - Vortex / Release:          release managers only -> Run build
    // Note that permissions inherited from <Root> apply here too and cannot be
    // revoked per-project, so "Run build" may already be granted more widely
    // than these assignments suggest. The trigger surface is the stronger
    // control: the only trigger fires on a pushed v* tag, which only Release
    // creates — so tag-push rights in GitHub are a second lock on releasing.
    subProject(CiProject)
    subProject(ReleaseProject)
    subProject(ToolsProject)
}

/**
 * CI sub-project: the per-OS builds plus the Package fan-out. Grant developers a
 * run-capable role here.
 */
object CiProject : Project({
    id("CI")
    name = "CI"
    description = "Per-OS build + test, and the release-installer fan-out"

    // Build + test a branch, on demand.
    buildType(BuildLinux)
    buildType(BuildWindows)
    buildType(BuildMacOS)

    // Fan-out: build release installers for each OS from a release tag.
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
    id("Release")
    name = "Release"
    description = "Restricted: mints + publishes a semantic version"

    buildType(Release)
})

/**
 * Tools sub-project: builds that produce inputs to the pipeline rather than
 * being part of it. Nothing here runs on a commit, and nothing else depends on
 * these builds — they are run by hand when an artifact needs regenerating.
 */
object ToolsProject : Project({
    id("Tools")
    name = "Tools"
    description = "Occasional builds that produce native artifacts, run by hand"

    buildType(BuildGdalMacOS)
})

/**
 * Git VCS root that treats nebula's release tags as branches.
 *
 * `branchSpec` exposes both ordinary branches and refs/tags/v* refs as logical
 * branches. The tag mapping (the capture group names the branch) is what lets a
 * tag push start the Package builds, and what lets the agent check the tag out
 * onto HEAD so nebula's useLastTag can read it. The default branch stays master.
 */
object VortexVcs : GitVcsRoot({
    id("VortexVcs")
    name = "Vortex"
    url = "https://github.com/HydrologicEngineeringCenter/Vortex.git"
    pushUrl = "https://github.com/HydrologicEngineeringCenter/Vortex.git"
    branch = "refs/heads/master"
    // A branch filter can only select from what this spec exposes, so anything
    // absent here is invisible to every build configuration — it will not appear
    // in the run dialog and no filter can match it.
    //
    //   +:refs/heads/*      every branch, so CI can be run against one. Without
    //                       it only `branch` above (master) is known.
    //   +:refs/tags/(v*)    release + rc tags (v1.2.3, v1.2.3-rc.1) as logical
    //                       branches, so a tag push can trigger the Package
    //                       builds. The (v*) capture names the logical branch,
    //                       which is what their +:v* trigger filter matches.
    branchSpec = """
        +:refs/heads/*
        +:refs/tags/(v*)
    """.trimIndent()
    // Required for the branchSpec above to match anything: TeamCity only reports
    // tag revisions when tags are treated as branches. Without it no tag ever
    // appears as a logical branch and the Package fan-out never fires.
    useTagsAsBranches = true
    userNameStyle = GitVcsRoot.UserNameStyle.NAME
    authMethod = password {
        userName = "%github.user%"
        password = "%github.token%"
    }
})

/**
 * Shared build logic for a single platform: extract the OS-specific native
 * libraries, run the test suite against them, then assemble the distribution
 * archive. Concrete per-OS configurations below add an agent requirement.
 * The `gradle.extraGradleParams` parameter lets the Package builds
 * inject -Prelease.useLastTag=true without duplicating the steps.
 *
 * `image` selects whether the Gradle steps run in a container. It cannot be a
 * build parameter resolved at run time, which is the natural way to express
 * this: TeamCity derives agent requirements from a build configuration without
 * resolving parameters, so a step carrying dockerImagePlatform contributes a
 * "Linux Docker server" requirement to every configuration using it, whatever
 * the image parameter would have evaluated to. Sharing one template across all
 * three platforms that way left Windows and macOS with no compatible agents at
 * all, since none of those agents run Docker. The distinction therefore has to
 * exist at generation time, which is why there are two templates below rather
 * than one template and a parameter.
 */
object Builders {
  /**
   * Lives inside a named object for the same reason Config's literals do: a
   * function declared at script level is a member of the script instance, and
   * an `object ... : Template({ ... })` calling it captures that instance,
   * which the Kotlin DSL compiler rejects.
   */
  fun platformBuild(bt: BuildTypeSettings, image: String) = with(bt) {
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

    // Inside the container the JDK comes from the image, so the runner must be
    // told nothing and will use its JAVA_HOME; on an agent it is the capability.
    val jdk = if (image.isEmpty()) "%env.JDK_21_0_x64%" else ""

    steps {
        if (image.isNotEmpty()) {
            // Build the toolchain image the Gradle steps below run inside.
            // Building it on the agent from the committed Dockerfile, rather
            // than pulling a tag from a registry, keeps
            // docker/linux-build/Dockerfile the only source of truth for the
            // Linux build environment — the approach hec-neptune-backend takes
            // for its packaging image.
            script {
                name = "Build the Linux toolchain image"
                scriptContent = "docker build -t $image docker/linux-build"
            }
        }
        // AGENTS.md: getNatives must run *before* tests, or every NetCDF-backed
        // test fails with UnsatisfiedLinkError. `clean` first for a fresh build.
        gradle {
            name = "Extract native libraries (getNatives)"
            tasks = "clean getNatives"
            useGradleWrapper = true
            gradleWrapperPath = ""
            jdkHome = jdk
            if (image.isNotEmpty()) {
                dockerImage = image
                dockerImagePlatform = GradleBuildStep.ImagePlatform.Linux
            }
        }
        // Run tests explicitly against the extracted natives. vortex-api sets
        // ignoreFailures = true, so Gradle exits 0 even on failures; TeamCity's
        // own test reporting (below) is what actually fails the build.
        gradle {
            name = "Test"
            tasks = "test"
            useGradleWrapper = true
            gradleWrapperPath = ""
            jdkHome = jdk
            if (image.isNotEmpty()) {
                dockerImage = image
                dockerImagePlatform = GradleBuildStep.ImagePlatform.Linux
            }
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
            jdkHome = jdk
            if (image.isNotEmpty()) {
                dockerImage = image
                dockerImagePlatform = GradleBuildStep.ImagePlatform.Linux
            }
        }
    }

    features {
        perfmon {}
    }

    // No requirements here. Gradle needs JDK 21 (the bundled runtime and the
    // Adoptium JRE dependencies are 21.0.9; source/target is 17), but where that
    // JDK comes from now differs by platform: Windows and macOS take it from the
    // agent's JDK_21_0_x64 capability, while Linux takes it from the container
    // image. A template requirement is inherited and cannot be removed by a
    // configuration that uses the template, so requiring the capability here
    // would keep the Linux builds pinned to the three agents that happen to have
    // a JDK installed — which is exactly what containerising them avoids. Each
    // configuration below therefore states its own requirements.

    failureConditions {
        executionTimeoutMin = 60
        // Default TeamCity behavior fails the build if any test fails — this is
        // what catches failures that Gradle swallows via ignoreFailures = true.
        testFailure = true
    }
  }
}

/** Windows and macOS: Gradle runs directly on the agent. */
object PlatformBuild : Template({
    id("PlatformBuild")
    name = "Platform Build"
    Builders.platformBuild(this, image = "")
})

/** Linux: the same steps, run inside the toolchain image. */
object PlatformBuildLinux : Template({
    id("PlatformBuildLinux")
    name = "Platform Build (Linux container)"
    Builders.platformBuild(this, image = Config.LINUX_IMAGE)
})

// ----------------------------------------------------------------------------
// CI builds — one per OS. Run by hand, picking the branch in the run dialog.
// The choices offered there come from the VCS root's branchSpec, not from
// anything here: a build configuration can only be run against a ref that spec
// exposes.
//
// These are deliberately NOT paused. Nothing triggers them — the file's only
// trigger is on the Package builds, filtered to release tags — so a CI build
// starts only when a person presses Run. Pausing would add no safety, and
// because UI editing is disabled and `paused` is a DSL property, it would mean
// running a build required editing this file and pushing.
// ----------------------------------------------------------------------------

/**
 * Shared literals.
 *
 * These live in a named object rather than at script level because settings.kts
 * is a *script*: a bare `val` becomes a property of the script instance, and any
 * `object BuildType({ ... })` referencing one then captures that instance, which
 * the Kotlin DSL compiler rejects. `const val` inside a named object is a
 * compile-time constant and is inlined at each use site, so nothing is captured.
 */
object Config {
    // -Prelease.useLastTag=true tells nebula to reuse the tag already on HEAD
    // instead of computing/creating a new one, so the installer version matches
    // the release exactly.
    const val USE_LAST_TAG = "-Prelease.useLastTag=true"

    // Trigger filter for the Package builds: only the tag "branches" (v1.2.3,
    // v1.2.3-rc.1, ...), never an ordinary branch. This is the one trigger in
    // the file, and it is deliberately this narrow.
    const val TAG_BRANCH_FILTER = "+:v*"

    // Tag of the Linux toolchain image. It is built on the agent from
    // docker/linux-build/Dockerfile by the first step of the template, so this
    // is a local tag and is never pushed or pulled; see that Dockerfile for what
    // the image provides and why.
    //
    // Pinning the toolchain in an image rather than installing it per agent is
    // the point: it makes the three nebula agents interchangeable, and lets the
    // build-linux agents be used too, as they have Docker but no JDK 21.
    const val LINUX_IMAGE = "vortex-linux-build:21"
}

object BuildLinux : BuildType({
    templates(PlatformBuildLinux)
    id("Build_Linux")
    name = "Build — Linux (x64)"

    // No JDK requirement: the image supplies it. Docker is what is needed.
    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
        contains("docker.server.osType", "linux")
    }
})

object BuildWindows : BuildType({
    templates(PlatformBuild)
    id("Build_Windows")
    name = "Build — Windows (x64)"

    requirements {
        contains("teamcity.agent.jvm.os.name", "Windows")
        exists("env.JDK_21_0_x64")
    }
})

object BuildMacOS : BuildType({
    templates(PlatformBuild)
    id("Build_macOS")
    name = "Build — macOS (x64)"

    requirements {
        contains("teamcity.agent.jvm.os.name", "Mac OS X")
        exists("env.JDK_21_0_x64")
    }
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
 * separate, conditional step that runs for `final` alone. A candidate gets a
 * tag and, through it, the per-OS installers from the Package builds, which is
 * what there is to test; putting one on a release repository would spend the
 * coordinate on a version that may never be released.
 */
object Release : BuildType({
    id("Release")
    name = "Release (nebula)"
    description = "Mint the semantic version; pushes the tag that drives the Package fan-out"

    // Not paused, for the same reason as the CI builds: with no triggers, this
    // runs only when someone deliberately runs it. Note it has more at stake
    // than the others — it mints and pushes a git tag.

    params {
        select(
            "release.task", "final",
            label = "Release task",
            description = "nebula.release task to run",
            options = listOf("final", "candidate", "snapshot")
        )
        // Which part of the version nebula increments. Left at the default it
        // infers the bump from the commits since the last tag.
        //
        // The option values are the flag itself rather than the bare word, so
        // that the default can contribute nothing to the command line. Passing
        // an empty -Prelease.scope= is not the same as omitting it, and this is
        // the one build where getting the version wrong means a pushed tag.
        select(
            "release.scope", "",
            label = "Version bump",
            description = "Part of the version to increment",
            options = listOf(
                "infer from commits" to "",
                "major" to "-Prelease.scope=major",
                "minor" to "-Prelease.scope=minor",
                "patch" to "-Prelease.scope=patch"
            )
        )
        // Anything else to pass to the release task, for the cases the two
        // parameters above do not cover.
        param("release.gradleParams", "")
    }

    vcs {
        root(VortexVcs)
        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        // Same toolchain image as the CI and Package builds. This build does not
        // use the PlatformBuild template, so the image step and the per-step
        // docker settings are repeated here rather than inherited. It needs the
        // image for the same reason they do: the publish step below compiles,
        // which resolves compileClasspath against artifacts.unidata.ucar.edu, so
        // it fails on an agent whose CA bundle predates that host's root — and
        // failing there is worse than in CI, because by then the tag is pushed.
        script {
            name = "Build the Linux toolchain image"
            scriptContent = "docker build -t ${Config.LINUX_IMAGE} docker/linux-build"
        }
        // Give the checkout a pushable origin. TeamCity authenticates its own
        // git calls by passing a credential helper per invocation, which is not
        // written into .git/config, so it does not reach the `git push` nebula
        // runs from inside the container -- build 3 tagged locally and then
        // failed with "could not read Password for https://tombrauer@github.com".
        //
        // Writing the token into the remote URL keeps it out of the process
        // arguments nebula constructs, and the checkout is deleted before every
        // run (cleanCheckout), so it does not outlive the build. TeamCity masks
        // the parameter in the log.
        script {
            name = "Authenticate the origin remote"
            scriptContent = """
                set -e
                git remote set-url origin \
                  "https://%github.user%:%github.token%@github.com/HydrologicEngineeringCenter/Vortex.git"
            """.trimIndent()
        }
        // Mint + push the tag only. Publishing is excluded here so that it is
        // driven entirely by the conditional step below (final only); -x build
        // also skips the OS-specific distribution.
        gradle {
            name = "Release / tag (%release.task%)"
            tasks = "%release.task%"
            gradleParams =
                "-x build -x :vortex-api:publish -x :vortex-ui:publish %release.scope% %release.gradleParams%"
            useGradleWrapper = true
            gradleWrapperPath = ""
            // Empty: JAVA_HOME comes from the image, as for the other builds.
            jdkHome = ""
            dockerImage = Config.LINUX_IMAGE
            dockerImagePlatform = GradleBuildStep.ImagePlatform.Linux
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
            jdkHome = ""
            dockerImage = Config.LINUX_IMAGE
            dockerImagePlatform = GradleBuildStep.ImagePlatform.Linux
            conditions {
                // Final only. A candidate is for testing the installers the
                // Package builds produce from its tag, and those come from the
                // artifacts, not from Nexus — publishing one puts a version on a
                // release repository that cannot be withdrawn if the candidate is
                // rejected, and maven-releases refuses redeployment, so the
                // coordinate is spent either way.
                equals("release.task", "final")
            }
        }
    }

    failureConditions {
        executionTimeoutMin = 60
        // nebula reports a failed tag push as a warning and exits zero, so build
        // 3 was green having created the tag only on the agent, which is then
        // deleted by the next clean checkout. A release that mints nothing is
        // the one outcome this build must not call success, and it is worse than
        // an ordinary failure: the Package builds wait on a tag that never
        // arrives, so the symptom appears somewhere else entirely.
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "Failed to push tag"
            failureMessage = "The tag was created locally but not pushed, so nothing will trigger the Package builds. Check the origin credentials."
            reverse = false
        }
    }

    // Pinned to Linux for determinism, and to Docker rather than to a JDK
    // capability: the JDK comes from the image, so an agent needs Docker and
    // nothing else. That widens this from the three agents carrying a JDK 21 to
    // every Linux agent, and makes them interchangeable rather than each having
    // to be provisioned identically by hand.
    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
        contains("docker.server.osType", "linux")
    }
})

// ----------------------------------------------------------------------------
// Package fan-out — one per OS, triggered by the tag Release pushes.
// ----------------------------------------------------------------------------

object PackageLinux : BuildType({
    templates(PlatformBuildLinux)
    id("Package_Linux")
    name = "Package — Linux (x64)"
    description = "Build the Linux release installer from the pushed tag"

    params { param("gradle.extraGradleParams", Config.USE_LAST_TAG) }
    triggers { vcs { branchFilter = Config.TAG_BRANCH_FILTER } }
    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
        contains("docker.server.osType", "linux")
    }
})

object PackageWindows : BuildType({
    templates(PlatformBuild)
    id("Package_Windows")
    name = "Package — Windows (x64)"
    description = "Build the Windows release installer from the pushed tag"

    params { param("gradle.extraGradleParams", Config.USE_LAST_TAG) }
    triggers { vcs { branchFilter = Config.TAG_BRANCH_FILTER } }
    requirements {
        contains("teamcity.agent.jvm.os.name", "Windows")
        exists("env.JDK_21_0_x64")
    }
})

object PackageMacOS : BuildType({
    templates(PlatformBuild)
    id("Package_macOS")
    name = "Package — macOS (x64)"
    description = "Build the macOS release installer from the pushed tag"

    params { param("gradle.extraGradleParams", Config.USE_LAST_TAG) }
    triggers { vcs { branchFilter = Config.TAG_BRANCH_FILTER } }
    requirements {
        contains("teamcity.agent.jvm.os.name", "Mac OS X")
        exists("env.JDK_21_0_x64")
    }
})

// ----------------------------------------------------------------------------
// Tools — not part of the pipeline. Run by hand when an artifact needs rebuilding.
// ----------------------------------------------------------------------------

/**
 * Builds native GDAL 3.2.1 with Java bindings for macOS, from source.
 *
 * Windows and Linux have always built against native GDAL 3.2.1. macOS ran
 * 3.5.0_1 because no macOS 3.2 build was published anywhere, and three tests
 * disagreed as a result. This produced the missing artifact, which is now on
 * the Nexus as org.gdal:gdal:3.2.1:macOS-x64, so all three platforms match.
 *
 * It stays here because the artifact may need rebuilding — for a new GDAL
 * version, or for arm64. See build-scripts/gdal-macos/build-gdal-macos.sh for
 * what it does and for what it had to work around.
 *
 * The output is not consumed automatically. The zips are published as build
 * artifacts and someone uploads them to the Nexus by hand, after which
 * build.gradle.kts points the macOS natives at them. The script prints the
 * coordinates and the repository to use when it finishes.
 */
object BuildGdalMacOS : BuildType({
    id("Build_Gdal_macOS")
    name = "Build GDAL 3.2.1 — macOS (x64)"
    description = "Compiles GDAL + PROJ from source with Java bindings; run by hand"

    params {
        // Kept outside the checkout so the GDAL and PROJ clones survive between
        // runs: the VCS root cleans all untracked files, which would otherwise
        // discard roughly a gigabyte of sources on every build.
        param("gdal.build.root", "%env.HOME%/gdal-build")
    }

    vcs {
        root(VortexVcs)
        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        script {
            name = "Build GDAL from source"
            workingDir = "."
            scriptContent = """
                set -e
                # Clear the artifact directory here rather than in the collect
                # step, because that step is skipped when this one fails while
                # publishing still runs. Build 9 failed in its first minute and
                # published build 8's three zips regardless, which is how a
                # bundle with no netCDF driver ends up looking like the output of
                # a build that never produced one.
                rm -rf "%teamcity.build.checkoutDir%/artifacts"
                mkdir -p "%gdal.build.root%"
                cd "%gdal.build.root%"
                "%teamcity.build.checkoutDir%/build-scripts/gdal-macos/build-gdal-macos.sh"
            """.trimIndent()
        }
        script {
            name = "Collect artifacts"
            scriptContent = """
                set -e
                mkdir -p artifacts
                cp -f "%gdal.build.root%/Output/"*.zip artifacts/
                ls -lh artifacts/
            """.trimIndent()
        }
    }

    artifactRules = "artifacts/*.zip"

    failureConditions {
        // Two source builds plus SWIG; the first run also clones GDAL and PROJ.
        executionTimeoutMin = 180
    }

    // macOS on x86_64: build-macOS is the Intel agent, and mac-studio-01-vm has
    // only an ARM64 JDK. The script's JAVA_HOME requirement is what the JDK
    // capability satisfies, and it defaults to building x86_64 to match.
    requirements {
        contains("teamcity.agent.jvm.os.name", "Mac OS X")
        exists("env.JDK_21_0_x64")
    }
})
