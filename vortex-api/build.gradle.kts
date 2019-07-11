plugins {
    java
}


repositories {
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    mavenCentral()
}

dependencies {
    implementation("org.gdal:gdal:2.4.0")
    implementation("javax.measure:unit-api:2.0-EDR")
    implementation("tech.units:indriya:2.0-EDR")
    implementation("systems.uom:systems-common:0.9")
    implementation("edu.ucar:netcdfAll:4.6.13")
    implementation("org.slf4j:slf4j-jdk14:1.7.25")
    implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
    testImplementation ("org.mockito:mockito-core:2.27.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

base.archivesBaseName = "vortex"
project.version = project.version.toString()

tasks.test {
    useJUnit()
    environment = mapOf("PATH" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin/gdal/plugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin/proj/SHARE")
    jvmArgs("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal-2-4-0/bin/gdal/java",
            "-Djava.io.tmpdir=C:/Temp")
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
