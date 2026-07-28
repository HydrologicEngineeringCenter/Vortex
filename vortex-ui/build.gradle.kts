import org.gradle.internal.jvm.Jvm
// Imported rather than written as java.io.ByteArrayOutputStream: inside this
// script `java` resolves to the Java plugin extension, which shadows the java.*
// package.
import java.io.ByteArrayOutputStream

plugins {
    java
    application
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    maven(url = "https://www.hec.usace.army.mil/nexus/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation(project(":vortex-api"))
    // Keep in step with vortex-api. The root project resolves both together, so
    // the higher declaration wins and can ship a binding ahead of its native —
    // which no test here would catch.
    implementation("org.gdal:gdal:3.2.0")
    implementation("com.formdev:flatlaf:3.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
}

tasks.jar {
    archiveBaseName.set("vortex-ui")
}
project.version = project.version.toString()

fun isWindows(): Boolean { return org.gradle.internal.os.OperatingSystem.current().isWindows }
fun isMacOsX(): Boolean { return org.gradle.internal.os.OperatingSystem.current().isMacOsX }
fun isLinux(): Boolean { return org.gradle.internal.os.OperatingSystem.current().isLinux }

fun uiJvmArgs(): List<String> {
    val binDir = "${rootProject.projectDir}/bin"
    var javaLibPath = ""
    var tempDirPath = ""

    if(isWindows()) {
        javaLibPath = "${binDir};${binDir}/gdal"
        tempDirPath = "C:/Temp"
    }

    if(isMacOsX()) {
        javaLibPath = "${binDir}/gdal:${binDir}/javaHeclib"
        tempDirPath = System.getenv("TMPDIR")
    }

    if(isLinux()) {
        javaLibPath = "/usr/lib/jni"
        tempDirPath = "/var/tmp"
    }

    return listOf("-Djava.library.path=${javaLibPath}", "-Djava.io.tmpdir=${tempDirPath}", "--add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED")
}

fun uiEnvironment(): Map<String,String> {
    if (isWindows()) {
        return mapOf(
            "PATH" to "${rootProject.projectDir}/bin/gdal;${rootProject.projectDir}/bin/netcdf",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"
        )
    }

    if (isMacOsX()) {
        return mapOf(
            "DYLD_LIBRARY_PATH" to "${rootProject.projectDir}/bin/gdal",
            "DYLD_FALLBACK_LIBRARY_PATH" to "@loader_path",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/proj-db"
        )
    }

    if (isLinux()) {
        return mapOf(
            "PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"
        )
    }

    return mapOf()
}

fun applicationTasks(): Map<String,String> {
    return mapOf(
        "calculator" to "-calculator",
        "clipper" to "-clipper",
        "gap-filler" to "-gap-filler",
        "grid-to-point" to "-grid-to-point",
        "image-exporter" to "-image-exporter",
        "importer" to "-importer",
        "normalizer" to "-normalizer",
        "sanitizer" to "-sanitizer",
        "time-shifter" to "-time-shifter",
        "time-step-resampler" to "-time-step-resampler"
    )
}

applicationTasks().forEach { (taskName, className) ->
    task(taskName, JavaExec::class) {
        group = "application"
        mainClass.set("mil.army.usace.hec.vortex.ui.VortexUi")
        args = listOf(className)
        classpath = sourceSets["main"].runtimeClasspath
        jvmArgs = uiJvmArgs()
        environment = uiEnvironment()
    }
}

tasks.test {
    jvmArgs = uiJvmArgs()
    environment = uiEnvironment()
}

// ---------------------------------------------------------------------------
// jpackage: builds the native wizard launchers for all three platforms,
// replacing the Launch4j configs that used to live under package/windows/*.xml
// (Windows) and the run-vortex.sh-based launchers (macOS/Linux).
//
// jpackage launchers can't set OS environment variables the way Launch4j's
// <var> entries or the shell scripts' `export` lines did, so PATH/DYLD_*/
// GDAL_DATA/PROJ_LIB are replaced with JVM system properties (read by
// GdalRegister) built from $APPDIR, a token the launcher exe substitutes at
// runtime with the app-image's app/ directory. Verified against the actual
// vendored natives:
//  - Windows: netcdf.dll lives in a separate bin/netcdf folder from gdal's
//    own dlls, and gdal302.dll depends on it, so it's preloaded via
//    System.load() before any GDAL call (see GdalRegister) instead of relying
//    on PATH.
//  - Linux: every vendored .so already has RUNPATH=$ORIGIN/. baked in (real
//    ELF inspection of the fetched artifacts confirmed this), so
//    java.library.path alone resolves everything; no LD_LIBRARY_PATH
//    equivalent is needed.
//  - macOS: the vendored dylibs reference every dependency as @rpath/... with
//    zero LC_RPATH entries of their own (confirmed via Mach-O inspection), so
//    without DYLD_LIBRARY_PATH nothing would resolve. Since jpackage can't set
//    that env var either, an @loader_path rpath is added to the staged gdal
//    dylibs with install_name_tool at packaging time instead.
// ---------------------------------------------------------------------------

fun jpackageWizards(): Map<String, String> {
    return mapOf(
        "calculator" to "-calculator",
        "clipper" to "-clipper",
        "gap-filler" to "-gap-filler",
        "grid-to-point-converter" to "-grid-to-point",
        "image-exporter" to "-image-exporter",
        "importer" to "-importer",
        "normalizer" to "-normalizer",
        "sanitizer" to "-sanitizer",
        "time-shifter" to "-time-shifter",
        "time-step-resampler" to "-time-step-resampler"
    )
}

// jpackage rejects nebula.release's "0.15.0-dev.0+<sha>" version strings; only
// the leading numeric dotted portion is kept.
fun jpackageAppVersion(): String {
    return Regex("^[0-9]+(\\.[0-9]+){0,2}").find(project.version.toString())?.value ?: "0.0.0"
}

fun jpackageJavaOptionsWindows(): List<String> {
    return listOf(
        "-Djava.library.path=\$APPDIR;\$APPDIR/gdal",
        "-Djava.io.tmpdir=C:/Temp",
        "--add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED",
        "-Dvortex.gdal.data=\$APPDIR/gdal/gdal-data",
        "-Dvortex.proj.lib=\$APPDIR/gdal/projlib"
    )
}

fun jpackageJavaOptionsLinux(): List<String> {
    return listOf(
        "-Djava.library.path=\$APPDIR:\$APPDIR/gdal:/usr/lib/jni",
        "--add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED",
        "-Dvortex.gdal.data=\$APPDIR/gdal/gdal-data",
        "-Dvortex.proj.lib=\$APPDIR/gdal/proj"
    )
}

fun jpackageJavaOptionsMacOS(): List<String> {
    return listOf(
        "-Djava.library.path=\$APPDIR:\$APPDIR/gdal",
        "--add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED",
        "-Dvortex.gdal.data=\$APPDIR/gdal-data",
        "-Dvortex.proj.lib=\$APPDIR/proj-db"
    )
}

fun jpackageExecutable(): String {
    return Jvm.current().getExecutable("jpackage").absolutePath
}

// jpackage marks its generated launcher executables read-only on Windows,
// where DeleteFile() refuses to remove a read-only file even for the owning
// process -- that makes `gradle clean` (and any other plain delete of this
// output) fail with "Failed to delete some children" the next time round.
// Clearing the attribute right after jpackage produces the files keeps them
// deletable without requiring manual intervention.
fun clearReadOnly(dir: java.io.File) {
    if (!dir.exists()) return
    dir.walkTopDown().forEach { it.setWritable(true) }
}

// Writes an --add-launcher properties file. jpackage's own parser (not plain
// java.util.Properties) drops single backslashes and doesn't accumulate
// repeated java-options/arguments lines, so paths use forward slashes and all
// java-options go on one combined, space-separated line (verified empirically
// against a real jpackage build before this was relied on for Windows).
fun writeJpackageLauncherProperties(
    file: java.io.File,
    jarName: String,
    arg: String,
    javaOptions: List<String>,
    icon: String
) {
    file.writeText(
        "main-class=mil.army.usace.hec.vortex.ui.VortexUi\n" +
            "main-jar=$jarName\n" +
            "arguments=$arg\n" +
            "java-options=${javaOptions.joinToString(" ")}\n" +
            "icon=$icon\n"
    )
}

// The primary launcher (vortex-ui[.exe]) gets no wizard argument, which falls
// through to VortexUi's AnyWizard picker; every wizard is an --add-launcher.
fun jpackageBaseCommand(
    name: String,
    inputDir: File,
    destDir: File,
    jarName: String,
    icon: String,
    runtimeImage: String
): MutableList<String> {
    return mutableListOf(
        jpackageExecutable(),
        "--type", "app-image",
        "--name", name,
        "--input", inputDir.absolutePath,
        "--dest", destDir.absolutePath,
        "--main-class", "mil.army.usace.hec.vortex.ui.VortexUi",
        "--main-jar", jarName,
        "--app-version", jpackageAppVersion(),
        "--vendor", "USACE HEC",
        "--icon", icon,
        "--runtime-image", runtimeImage
    )
}

// ---------------------------------------------------------------------------
// Windows
// ---------------------------------------------------------------------------

// jpackage's --add-launcher properties-file parser silently mangles single
// backslashes (verified empirically), so $projectDir (which renders with
// native \ separators on Windows) must be normalized to forward slashes here
// -- otherwise every add-launcher silently falls back to jpackage's default
// icon instead of the custom one, while the primary launcher (icon passed as
// a plain CLI arg, not through a properties file) looks fine and masks it.
val jpackageIconWindows = "$projectDir/package/windows/vortex_black.ico".replace("\\", "/")
val jpackageInputDirWindows = layout.buildDirectory.dir("jpackage/windows/input")
val jpackageLaunchersDirWindows = layout.buildDirectory.dir("jpackage/windows/launchers")
val jpackageOutputDirWindows = layout.buildDirectory.dir("jpackage/windows/output")

val jpackageStageWindows = tasks.register<Copy>("jpackageStageWindows") {
    group = "distribution"
    description = "Stages the UI jar, GDAL/netCDF natives, and javaHeclib for jpackage."
    dependsOn(tasks.jar, ":refreshNatives", ":getNatives")

    into(jpackageInputDirWindows)
    from(tasks.jar)
    from(configurations.runtimeClasspath) { include("*.jar") }
    from("${rootProject.projectDir}/bin/javaHeclib.dll")
    into("gdal") { from("${rootProject.projectDir}/bin/gdal") }
    into("netcdf") { from("${rootProject.projectDir}/bin/netcdf") }
}

tasks.register<Exec>("jpackageWindows") {
    group = "distribution"
    description = "Builds the Windows wizard launcher .exe files with jpackage."
    dependsOn(jpackageStageWindows)
    inputs.dir(jpackageInputDirWindows)
    outputs.dir(jpackageOutputDirWindows)

    doFirst {
        val launchersDir = jpackageLaunchersDirWindows.get().asFile
        delete(launchersDir)
        launchersDir.mkdirs()
        delete(jpackageOutputDirWindows)

        val jarName = tasks.jar.get().archiveFileName.get()
        val javaOptions = jpackageJavaOptionsWindows()

        val command = jpackageBaseCommand(
            "vortex-ui",
            jpackageInputDirWindows.get().asFile,
            jpackageOutputDirWindows.get().asFile,
            jarName,
            jpackageIconWindows,
            "${rootProject.projectDir}/bin/jre"
        )
        javaOptions.forEach { command += listOf("--java-options", it) }

        jpackageWizards().forEach { (name, arg) ->
            val propsFile = launchersDir.resolve("$name.properties")
            writeJpackageLauncherProperties(propsFile, jarName, arg, javaOptions, jpackageIconWindows)
            command += listOf("--add-launcher", "$name=${propsFile.absolutePath}")
        }

        commandLine = command
    }

    doLast {
        clearReadOnly(jpackageOutputDirWindows.get().asFile)
    }
}

// ---------------------------------------------------------------------------
// Linux
// ---------------------------------------------------------------------------

val jpackageIconLinux = "$projectDir/src/main/resources/images/vortex_black.png".replace("\\", "/")
val jpackageInputDirLinux = layout.buildDirectory.dir("jpackage/linux/input")
val jpackageLaunchersDirLinux = layout.buildDirectory.dir("jpackage/linux/launchers")
val jpackageOutputDirLinux = layout.buildDirectory.dir("jpackage/linux/output")

val jpackageStageLinux = tasks.register<Copy>("jpackageStageLinux") {
    group = "distribution"
    description = "Stages the UI jar, GDAL natives, and javaHeclib for jpackage."
    dependsOn(tasks.jar, ":refreshNatives", ":getNatives")

    into(jpackageInputDirLinux)
    from(tasks.jar)
    from(configurations.runtimeClasspath) { include("*.jar") }
    from("${rootProject.projectDir}/bin/libjavaHeclib.so")
    into("gdal") { from("${rootProject.projectDir}/bin/gdal") }
}

tasks.register<Exec>("jpackageLinux") {
    group = "distribution"
    description = "Builds the Linux wizard launchers with jpackage."
    dependsOn(jpackageStageLinux)
    inputs.dir(jpackageInputDirLinux)
    outputs.dir(jpackageOutputDirLinux)

    doFirst {
        val launchersDir = jpackageLaunchersDirLinux.get().asFile
        delete(launchersDir)
        launchersDir.mkdirs()
        delete(jpackageOutputDirLinux)

        val jarName = tasks.jar.get().archiveFileName.get()
        val javaOptions = jpackageJavaOptionsLinux()

        val command = jpackageBaseCommand(
            "vortex-ui",
            jpackageInputDirLinux.get().asFile,
            jpackageOutputDirLinux.get().asFile,
            jarName,
            jpackageIconLinux,
            "${rootProject.projectDir}/bin/jre"
        )
        javaOptions.forEach { command += listOf("--java-options", it) }

        jpackageWizards().forEach { (name, arg) ->
            val propsFile = launchersDir.resolve("$name.properties")
            writeJpackageLauncherProperties(propsFile, jarName, arg, javaOptions, jpackageIconLinux)
            command += listOf("--add-launcher", "$name=${propsFile.absolutePath}")
        }

        commandLine = command
    }

    doLast {
        clearReadOnly(jpackageOutputDirLinux.get().asFile)
    }
}

// ---------------------------------------------------------------------------
// macOS
// ---------------------------------------------------------------------------

val jpackageIconMacOS = "$projectDir/package/macOS/vortex_black.icns".replace("\\", "/")
val jpackageInputDirMacOS = layout.buildDirectory.dir("jpackage/macOS/input")
val jpackageLaunchersDirMacOS = layout.buildDirectory.dir("jpackage/macOS/launchers")
val jpackageOutputDirMacOS = layout.buildDirectory.dir("jpackage/macOS/output")

val jpackageStageMacOS = tasks.register<Copy>("jpackageStageMacOS") {
    group = "distribution"
    description = "Stages the UI jar, GDAL natives, and javaHeclib for jpackage."
    dependsOn(tasks.jar, ":refreshNatives", ":getNatives")

    into(jpackageInputDirMacOS)
    from(tasks.jar)
    from(configurations.runtimeClasspath) { include("*.jar") }
    from("${rootProject.projectDir}/bin/libjavaHeclib.dylib")
    into("gdal") { from("${rootProject.projectDir}/bin/gdal") }
    into("gdal-data") { from("${rootProject.projectDir}/bin/gdal-data") }
    into("proj-db") { from("${rootProject.projectDir}/bin/proj-db") }
}

// A dylib that names its siblings as @rpath/libfoo.dylib needs an LC_RPATH of
// its own, and a jpackage launcher cannot supply one the way the old
// run-vortex.sh could through DYLD_LIBRARY_PATH. That was the state of the
// 3.5.0_1 bundle this was written for.
//
// The GDAL 3.2.1 bundle is built with @loader_path already set on every library
// and each one re-signed, so against it this finds nothing to do. Adding the
// rpath unconditionally was worse than redundant: install_name_tool invalidates
// a Mach-O signature, so it errored on every library that already had the rpath
// and silently broke the signature of every library it did modify. Both were
// swallowed by isIgnoreExitValue, leaving a bundle that packaged cleanly and
// carried libraries macOS may refuse to load.
//
// So check first, modify only what is missing the rpath, and re-sign what is
// modified. The check stays rather than the task being deleted, because a future
// bundle without rpaths would fail to load and this is what would catch it.
//
// Requires the Xcode Command Line Tools: otool, install_name_tool and codesign.
val jpackageFixMacRpaths = tasks.register("jpackageFixMacRpaths") {
    group = "distribution"
    description = "Ensures the staged macOS gdal dylibs carry an @loader_path rpath."
    dependsOn(jpackageStageMacOS)

    doLast {
        fileTree(jpackageInputDirMacOS.get().asFile.resolve("gdal")) {
            include("*.dylib")
        }.forEach { dylib ->
            val loadCommands = ByteArrayOutputStream()
            exec {
                commandLine("otool", "-l", dylib.absolutePath)
                standardOutput = loadCommands
            }

            // Look for an LC_RPATH entry specifically. Searching the whole output
            // for "@loader_path" would also match a dependency recorded as
            // @loader_path/libfoo.dylib, which is not an rpath and would make
            // every library look as though it already had one.
            val lines = loadCommands.toString().lines()
            val hasLoaderPath = lines.indices.any { i ->
                lines[i].contains("cmd LC_RPATH") &&
                        lines.drop(i + 1).take(3).any { it.trim().startsWith("path @loader_path") }
            }
            if (hasLoaderPath) return@forEach

            logger.lifecycle("adding @loader_path rpath to ${dylib.name}")
            exec { commandLine("install_name_tool", "-add_rpath", "@loader_path", dylib.absolutePath) }
            exec { commandLine("codesign", "--force", "--sign", "-", dylib.absolutePath) }
        }
    }
}

tasks.register<Exec>("jpackageMacOS") {
    group = "distribution"
    description = "Builds the macOS wizard launchers with jpackage."
    dependsOn(jpackageFixMacRpaths)
    inputs.dir(jpackageInputDirMacOS)
    outputs.dir(jpackageOutputDirMacOS)

    doFirst {
        val launchersDir = jpackageLaunchersDirMacOS.get().asFile
        delete(launchersDir)
        launchersDir.mkdirs()
        delete(jpackageOutputDirMacOS)

        val jarName = tasks.jar.get().archiveFileName.get()
        val javaOptions = jpackageJavaOptionsMacOS()

        val command = jpackageBaseCommand(
            "vortex-ui",
            jpackageInputDirMacOS.get().asFile,
            jpackageOutputDirMacOS.get().asFile,
            jarName,
            jpackageIconMacOS,
            "${rootProject.projectDir}/bin/jre/Contents/Home"
        )
        javaOptions.forEach { command += listOf("--java-options", it) }

        jpackageWizards().forEach { (name, arg) ->
            val propsFile = launchersDir.resolve("$name.properties")
            writeJpackageLauncherProperties(propsFile, jarName, arg, javaOptions, jpackageIconMacOS)
            command += listOf("--add-launcher", "$name=${propsFile.absolutePath}")
        }

        commandLine = command
    }

    doLast {
        clearReadOnly(jpackageOutputDirMacOS.get().asFile)
    }
}

tasks.named<Test>("test") {
    ignoreFailures = true
    useJUnitPlatform()
}

distributions.main {
    contents {
        from("package") {
            include("*.sh")
            into("scripts")
        }

        from(tasks.getByPath(":refreshNatives")) {
            into("bin")
        }
    }
}

val mavenUser: String by project
val mavenPassword: String by project

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "mil.army.usace.hec"
            artifactId = "vortex-ui"

            from(components["java"])
        }
    }
    repositories {
        maven {
            credentials {
                username = "$mavenUser"
                password = "$mavenPassword"
            }
            val releasesRepoUrl = uri("https://www.hec.usace.army.mil/nexus/repository/maven-releases/")
            val snapshotsRepoUrl = uri("https://www.hec.usace.army.mil/nexus/repository/maven-snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

tasks.getByName("publish").dependsOn("jar")
tasks.getByName("startScripts").enabled = false
tasks.getByName("distTar").enabled = false
// The application plugin's default distZip (just the jar + dependency libs,
// no natives, no jpackage launchers) isn't part of the actual release
// packaging (that's jpackage + the root project's zipWin/zipLinux/zipMacOS)
// and its similar name/location next to the real artifact is confusing.
tasks.getByName("distZip").enabled = false