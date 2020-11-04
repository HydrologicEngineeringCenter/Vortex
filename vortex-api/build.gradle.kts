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
    implementation("mil.army.usace.hec:hec:6.0.0.51")
    implementation("mil.army.usace.hec:heclib:6.0.0.51")
    implementation("mil.army.usace.hec:hecData:6.0.0.51")
    implementation("org.gdal:gdal:2.4.0")
    implementation("org.locationtech.jts:jts-core:1.16.1")
    implementation("tech.units:indriya:2.0.4")
    implementation("systems.uom:systems-common:2.0.2")
    implementation("edu.ucar:cdm-core:5.4.0-SNAPSHOT")
    runtimeOnly("com.rmanet:rma:6.0.0.51")
    runtimeOnly("edu.ucar:grib:5.4.0-SNAPSHOT")
    runtimeOnly("edu.ucar:netcdf4:5.4.0-SNAPSHOT")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.25")
    testImplementation ("org.mockito:mockito-core:2.27.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
}

base.archivesBaseName = "vortex"
project.version = project.version.toString()

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
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
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
            val releasesRepoUrl = uri("https://www.hec.usace.army.mil/nexus/repository/maven-releases/")
            val snapshotsRepoUrl = uri("https://www.hec.usace.army.mil/nexus/repository/maven-snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

tasks.getByName("publish").dependsOn("jar")
