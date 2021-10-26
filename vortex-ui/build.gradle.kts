plugins {
    java
    id("maven-publish")
}


repositories {
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    maven(url = "https://www.hec.usace.army.mil/nexus/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation(project(":vortex-api"))
    implementation("mil.army.usace.hec:hec:6.0.0.51")
    implementation("mil.army.usace.hec:heclib:6.0.0.51")
    implementation("mil.army.usace.hec:hecData:6.0.0.51")
    implementation("org.gdal:gdal:3.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
}

base.archivesBaseName = "vortex"
project.version = project.version.toString()

task("calculator", JavaExec::class) {
    group = "application"
    main = "mil.army.usace.hec.vortex.ui.CalculatorWizard"
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
    environment(mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"))
}

task("clipper", JavaExec::class) {
    group = "application"
    main = "mil.army.usace.hec.vortex.ui.ClipperWizard"
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
    environment(mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"))
}

task("importer", JavaExec::class) {
    group = "application"
    main = "mil.army.usace.hec.vortex.ui.ImportMetWizard"
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
    environment(mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"))
}

task("normalizer", JavaExec::class) {
    group = "application"
    main = "mil.army.usace.hec.vortex.ui.NormalizerWizard"
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
    environment(mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
        "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
        "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
        "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"))
}

task("grid-to-point", JavaExec::class) {
    group = "application"
    main = "mil.army.usace.hec.vortex.ui.GridToPointWizard"
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
    environment(mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"))
}

task("sanitizer", JavaExec::class) {
    group = "application"
    main = "mil.army.usace.hec.vortex.ui.SanitizerWizard"
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
    environment(mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"))
}

task("shifter", JavaExec::class) {
    group = "application"
    main = "mil.army.usace.hec.vortex.ui.ShifterWizard"
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
    environment(mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib"))
}

tasks.test {
    useJUnit()

    if(org.gradle.internal.os.OperatingSystem.current().isWindows()) {
        environment = mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
                "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdal/gdalplugins",
                "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
                "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib")

        jvmArgs("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal",
                "-Djava.io.tmpdir=C:/Temp")
    }
    else if(org.gradle.internal.os.OperatingSystem.current().isLinux()) {
        jvmArgs("-Djava.library.path=/usr/lib/jni",
                "-Djava.io.tmpdir=/var/tmp")
    }
    else if(org.gradle.internal.os.OperatingSystem.current().isMacOsX()) {
        jvmArgs("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal@2.4.4/2.4.4_1/lib",
                "-Djava.io.tmpdir=/var/tmp")
    }
}

tasks.named<Test>("test") {
    ignoreFailures = true
    useJUnitPlatform()
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