plugins {
    java
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
    implementation("edu.ucar:netcdf4")
    constraints {
        implementation("edu.ucar:netcdf4:5.5.3")
        runtimeOnly("net.java.dev.jna:jna:5.13.0")
    }
    
    implementation("mil.army.usace.hec:hec-monolith:3.+") {
        isTransitive = false
    }
    implementation("mil.army.usace.hec:hec-nucleus-data:2.+")
    implementation("mil.army.usace.hec:hec-nucleus-metadata:2.+")
    // The Java binding must match the native GDAL that getNatives extracts for
    // this platform (see the root build.gradle.kts): 3.2.1 on Linux and Windows,
    // 3.5.0_1 on macOS. Declare exactly one version — an unconditional
    // declaration alongside these would win wherever it is higher, because
    // Gradle resolves version conflicts by taking the highest, and a binding
    // ahead of its native fails at runtime on a symbol the native lacks.
    // This mirrors hec-hms, which pairs the same versions.
    if (org.gradle.internal.os.OperatingSystem.current().isLinux()) {
        implementation("org.gdal:gdal:3.2.0")
    } else if (org.gradle.internal.os.OperatingSystem.current().isMacOsX()) {
        implementation("org.gdal:gdal:3.5.0")
    } else {
        implementation("org.gdal:gdal:3.2.0")
    }
    implementation("org.locationtech.jts:jts-core:1.19.0")
    implementation("tech.units:indriya:2.1.4")
    implementation("systems.uom:systems-common:2.1")
    implementation("edu.ucar:cdm-core:5.5.3")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.hdfgroup:jarhdf5:3.3.2")
    //start runtime-only deps required by HEC shared libraries
    runtimeOnly("com.google.flogger:flogger:0.7.4")
    runtimeOnly("com.google.flogger:flogger-system-backend:0.7.4")
    //end runtime-only deps required by HEC shared libraries
    runtimeOnly("edu.ucar:grib:5.5.3")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.25")
    testImplementation("org.mockito:mockito-core:2.27.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.2")
}

project.version = project.version.toString()

tasks.jar {
    archiveBaseName.set("vortex")
}

tasks.test {
    if (org.gradle.internal.os.OperatingSystem.current().isWindows()) {
        environment = mapOf(
            "PATH" to "${rootProject.projectDir}/bin/gdal;${rootProject.projectDir}/bin/netcdf;${rootProject.projectDir}/bin/hdf",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"
        )

        jvmArgs(
            "-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal;${rootProject.projectDir}/bin/hdf",
            "-Djava.io.tmpdir=C:/Temp"
        )
    } else if (org.gradle.internal.os.OperatingSystem.current().isLinux()) {
        jvmArgs(
            "-Djava.library.path=${rootProject.projectDir}/bin" +
                    ":${rootProject.projectDir}/bin/gdal" +
                    ":/usr/lib/jni",
            // netcdf-java finds the netCDF C library through JNA, which reads
            // jna.library.path and not java.library.path. Point it at the GDAL
            // bundle, where getNatives leaves a libnetcdf.so symlink, so the
            // netCDF and curl/nghttp2 libraries all come from the one set that
            // was built together.
            "-Djna.library.path=${rootProject.projectDir}/bin/gdal",
            "-Djava.io.tmpdir=/var/tmp"
        )
        // Deliberately no LD_LIBRARY_PATH. It is not needed —
        // libgdalalljni.so carries $ORIGIN in its search path and so finds its
        // siblings relative to itself — and it is actively risky, because
        // LD_LIBRARY_PATH takes precedence over the default search path rather
        // than acting as a fallback. Setting it to bin/gdal against the older
        // 3.0.4 bundle, which shipped its own glibc, resolved the test JVM's
        // libc to GDAL's copy and the JVM failed to start with exit 127. The
        // 3.2.1 bundle no longer ships a glibc, but the precedence remains, so
        // do not reintroduce it. Note macOS below uses the fallback variable,
        // DYLD_FALLBACK_LIBRARY_PATH, for exactly this reason.
        environment("GDAL_DATA", "${rootProject.projectDir}/bin/gdal/gdal-data")
        // Named "proj" in the Linux bundle, unlike "projlib" on Windows.
        environment("PROJ_LIB", "${rootProject.projectDir}/bin/gdal/proj")
    } else if (org.gradle.internal.os.OperatingSystem.current().isMacOsX()) {
        jvmArgs(
            "-Djava.library.path=${rootProject.projectDir}/bin/gdal:${rootProject.projectDir}/bin/hdf:${rootProject.projectDir}/bin/javaHeclib",
            "-Djava.io.tmpdir=${System.getenv("TMPDIR")}"
        )
        environment = mapOf(
            "PATH" to "${rootProject.projectDir}/bin/hdf",
            "DYLD_FALLBACK_LIBRARY_PATH" to "@loader_path",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/proj-db"
        )
    }
}

tasks.named<Test>("test") {
    ignoreFailures = true
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set(rootProject.name + "-all")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.getByName("build").dependsOn("fatJar")

val mavenUser: String by project
val mavenPassword: String by project

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "mil.army.usace.hec"
            artifactId = "vortex"

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
