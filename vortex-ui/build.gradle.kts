plugins {
    java
    application
    id("maven-publish")
}


repositories {
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    maven(url = "https://www.hec.usace.army.mil/nexus/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation(project(":vortex-api"))
    implementation("org.gdal:gdal:3.2.0")
    implementation("com.formdev:flatlaf:3.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
}

base.archivesBaseName = "vortex-ui"
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
        "grid-to-point" to "-grid-to-point",
        "image-exporter" to "-image-exporter",
        "importer" to "-importer",
        "normalizer" to "-normalizer",
        "sanitizer" to "-sanitizer",
        "shifter" to "-shifter"
    )
}

applicationTasks().forEach { (taskName, className) ->
    task(taskName, JavaExec::class) {
        group = "application"
        main = "mil.army.usace.hec.vortex.ui.VortexUi"
        args = listOf(className)
        classpath = sourceSets["main"].runtimeClasspath
        jvmArgs = uiJvmArgs()
        environment = uiEnvironment()
    }
}

tasks.test {
    useJUnit()
    jvmArgs = uiJvmArgs()
    environment = uiEnvironment()
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