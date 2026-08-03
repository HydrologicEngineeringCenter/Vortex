import org.gradle.internal.os.OperatingSystem
// Imported because inside this script `java` resolves to the Java plugin
// extension, which shadows the java.* package and makes java.nio.file.* fail.
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    java
    id("nebula.release") version "19.0.10"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

val windows_x64 by configurations.creating
val linux_x64 by configurations.creating
val macOS_x64 by configurations.creating
val macOS_aarch64 by configurations.creating

repositories {
    maven(url = "https://www.hec.usace.army.mil/nexus/repository/maven-public/")
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    mavenCentral()
}

dependencies {
    implementation(project(":vortex-api"))
    implementation(project(":vortex-ui"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.2")
    windows_x64("mil.army.usace.hec:javaHeclib:7-IU-16-win-x86_64@zip")
    windows_x64("org.gdal:gdal:3.2.1-win-x64@zip")
    windows_x64("edu.ucar:netcdf:4.9.2:win-x64@zip")
    windows_x64 ("org.hdfgroup:hdf:1.14.0-win-x64@zip")
    linux_x64("mil.army.usace.hec:javaHeclib:7-IU-16-linux-x86_64-full@tar.gz")
    linux_x64("org.gdal:gdal:3.2.1:linux@tar.gz")
    linux_x64("org.hdfgroup:hdf:2.14.0-linux64@tar.gz")
    // These three move together. The macOS gdal zip is dylibs only, where the
    // Windows and Linux archives carry their data inside, and PROJ 7 data is not
    // interchangeable with PROJ 9's.
    macOS_x64("org.gdal:gdal:3.2.1:macOS-x64@zip")
    macOS_x64("org.gdal:gdal-data:3.2.1@zip")
    macOS_x64("org.proj:proj-db:7.2.1@zip")
    macOS_x64("mil.army.usace.hec:javaHeclib:7-IU-16-macOS-x86_64-full@zip")
    macOS_x64("org.hdfgroup:hdf:1.14.0:macOS-x64@zip")
}

// All three platforms' wizard launchers are now built by jpackage (see
// vortex-ui/build.gradle.kts) instead of being copied from committed
// Launch4j output (Windows) or shell scripts (macOS/Linux). jpackage's
// app-image bundles the jars, runtime, and GDAL natives itself, so there's
// no separate lib/bin/jre copy step for any OS anymore.
fun jpackagePlatformDir(): String {
    return when {
        OperatingSystem.current().isWindows() -> "windows"
        OperatingSystem.current().isMacOsX() -> "macOS"
        else -> "linux"
    }
}

fun jpackageTaskName(): String {
    return when {
        OperatingSystem.current().isWindows() -> "jpackageWindows"
        OperatingSystem.current().isMacOsX() -> "jpackageMacOS"
        else -> "jpackageLinux"
    }
}

// jpackage app-image names its output "vortex-ui" everywhere except macOS,
// where app-image output is always a "<name>.app" bundle.
fun jpackageAppImageName(): String {
    return if (OperatingSystem.current().isMacOsX()) "vortex-ui.app" else "vortex-ui"
}

tasks.register<Copy>("copyJpackageLaunchers") {
    dependsOn(":vortex-ui:build", ":vortex-ui:${jpackageTaskName()}")
    from(project(":vortex-ui").layout.buildDirectory.dir("jpackage/${jpackagePlatformDir()}/output/${jpackageAppImageName()}")) {
        // macOS only. from(dir) copies what is inside dir, which is right for
        // the Windows and Linux app images -- their launchers and app/ and
        // runtime/ are meant to sit at the top of the distribution. On macOS the
        // app image is a .app bundle, and a bundle is a directory the operating
        // system presents as a single file, so copying its contents leaves a
        // bare Contents/ in the distribution and nothing that will launch.
        // Nest it back under its own name to keep the bundle intact.
        if (OperatingSystem.current().isMacOsX()) {
            into(jpackageAppImageName())
        }
    }
    into(layout.buildDirectory.dir("distributions/${rootProject.name}-${project.version}"))
}

// macOS produces a disk image rather than a directory to be archived, so it does
// not go through copyJpackageLaunchers and the zip tasks at all. jpackage names
// the file after the app and its app-version; rename it to the convention the
// other platforms use, which is also what the TeamCity artifact rules match.
tasks.register<Copy>("copyMacOSInstaller") {
    dependsOn(":vortex-ui:build", ":vortex-ui:jpackageMacOS")
    from(project(":vortex-ui").layout.buildDirectory.dir("jpackage/macOS/output")) {
        include("*.dmg")
        rename { "${rootProject.name}-${project.version}-macOS-x64.dmg" }
    }
    into(layout.buildDirectory.dir("distributions"))
}

tasks.register<Copy>("copyLicense") {
    from(project.rootDir) {
        include("LICENSE.md")
    }
    into(layout.buildDirectory.dir("distributions/${rootProject.name}-${project.version}"))
}

tasks.register<Copy>("copyFatJar") {
    from(project(":vortex-api").layout.buildDirectory.dir("libs")) {
        include("${rootProject.name}-all-${project.version}")
    }
    into(layout.buildDirectory.dir("distributions"))
}

tasks.register<Copy>("getNatives") {
    if (OperatingSystem.current().isWindows()) {
        configurations.getByName("windows_x64").asFileTree.forEach() {
            from(zipTree(it))
            into("$projectDir/bin")
        }
    } else if (OperatingSystem.current().isLinux()) {
        configurations.getByName("linux_x64").asFileTree.forEach() {
            from(tarTree(it))
            into("$projectDir/bin")
        }
    } else if (OperatingSystem.current().isMacOsX()) {
        configurations.getByName("macOS_x64").asFileTree.forEach() {
            from(zipTree(it))
            into("$projectDir/bin")
        }
    }

    doLast {
        if (OperatingSystem.current().isLinux()) {
            // netcdf-java asks JNA for a library named "netcdf", which resolves
            // to an unversioned libnetcdf.so. GDAL's Linux bundle ships only a
            // versioned libnetcdf.so.<n>, so add the unversioned name alongside
            // it and let the tests point jna.library.path here.
            //
            // Using GDAL's copy rather than the distribution's is the point.
            // GDAL loads its own libnghttp2.so.14 into the process via $ORIGIN,
            // and the distribution's libnetcdf pulls in the system libcurl,
            // which needs a symbol added in a later nghttp2 than that. The two
            // share a soname, GDAL's loads first, and the netCDF load then
            // fails on the undefined symbol. Everything under bin/gdal was
            // built together and is consistent with itself.
            val gdalDir = file("$projectDir/bin/gdal")
            val versioned = gdalDir.listFiles { f: File -> f.name.startsWith("libnetcdf.so.") }?.firstOrNull()
            val link = File(gdalDir, "libnetcdf.so")
            if (versioned != null && !link.exists()) {
                Files.createSymbolicLink(link.toPath(), Paths.get(versioned.name))
            }
        } else if (OperatingSystem.current().isMacOsX()) {
            // The javaHeclib macOS archive is self-inconsistent: its
            // libjavaHeclib.dylib asks for @rpath/libgfortran.dylib but only
            // libgfortran.5.dylib is packaged, so dlopen fails and every
            // DSS-backed test dies. Add the name it asks for. A symlink, not a
            // copy: Gradle's Copy dereferences it into the jpackage bundle, so
            // the shipped app still gets a real file under both names.
            val heclibDir = file("$projectDir/bin/javaHeclib")
            val versioned = heclibDir.listFiles { f: File -> f.name.startsWith("libgfortran.") }?.firstOrNull()
            val link = File(heclibDir, "libgfortran.dylib")
            if (versioned != null && !link.exists()) {
                Files.createSymbolicLink(link.toPath(), Paths.get(versioned.name))
            }
        }
    }
}

tasks.register<Delete>("refreshNatives") {
    destroyables.register("$projectDir/x64")
    doLast {
        delete("$projectDir/bin")
    }
}
tasks.getByName("refreshNatives") { finalizedBy("getNatives") }

tasks.register<Tar>("zipLinux") {
    archiveFileName.set("${rootProject.name}-${project.version}-linux-x64" + ".tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("distributions").get().asFile)
    from(layout.buildDirectory.dir("distributions/${rootProject.name}-${project.version}"))
    into("${rootProject.name}-${project.version}")
    compression = Compression.GZIP
}

tasks.register<Zip>("zipWin") {
    archiveFileName.set("${rootProject.name}-${project.version}-win-x64" + ".zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions").get().asFile)
    from(layout.buildDirectory.dir("distributions/${rootProject.name}-${project.version}"))
    into("${rootProject.name}-${project.version}")
}

tasks.register("zip") {
    if (OperatingSystem.current().isWindows()) {
        dependsOn("zipWin")
    } else if (OperatingSystem.current().isLinux()) {
        dependsOn("zipLinux")
    }
    // macOS has nothing to archive. jpackage built the disk image and
    // copyMacOSInstaller has already put it in build/distributions.
}

// macOS assembles nothing: the disk image jpackage produced is the deliverable,
// and copyMacOSInstaller puts it beside the other platforms' archives. The
// licence travels inside the bundle for that platform, staged by
// jpackageStageMacOS, since there is no directory here to place it next to.
if (OperatingSystem.current().isMacOsX()) {
    tasks.getByName("build").dependsOn("copyMacOSInstaller")
} else {
    tasks.getByName("build").dependsOn("copyJpackageLaunchers")
    tasks.getByName("build").dependsOn("copyLicense")
}
tasks.getByName("build").dependsOn("vortex-api:fatJar")
tasks.getByName("build").dependsOn("copyFatJar")
tasks.getByName("build").finalizedBy("zip")
val distributionInputs = listOf(
    "copyJpackageLaunchers", "copyLicense", "copyFatJar"
)
listOf("zipWin", "zipLinux").forEach { zipTask ->
    tasks.getByName(zipTask).dependsOn(distributionInputs)
}

tasks.matching { it.name.contains("final") }.forEach { it.dependsOn(":build") }
tasks.matching { it.name.contains("final") }.forEach { it.dependsOn("vortex-api:publish") }
tasks.matching { it.name.contains("final") }.forEach { it.dependsOn("vortex-ui:publish") }
tasks.matching { it.name.contains("candidate") }.forEach { it.dependsOn(":build") }

tasks.getByName("jar").enabled = false

tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    maxHeapSize = "2g"
}
