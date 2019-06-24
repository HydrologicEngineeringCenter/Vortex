plugins {
    java
    application
}

base.archivesBaseName = "importer"
val version = project.version.toString()

repositories {
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    mavenCentral()
}

dependencies {
    implementation(project(":vortex-api"))
    implementation("org.slf4j:slf4j-jdk14:1.7.25")
    implementation("com.google.inject:guice:4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveVersion.set("")
    manifest {
        attributes(
                "Main-Class" to "importer.MetDataImportWizard",
                "Class-Path" to "./lib/*"
        )
    }
}

tasks.register<Copy>("copyJar"){
    dependsOn("jar")
    from ("$buildDir/libs") {
        include ("importer.jar")
    }
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/${base.archivesBaseName}-$version")
}

tasks.register<Copy>("copyResources"){
    from ("$projectDir/src/main/deploy/package/windows"){
        exclude ("*.ico")
        exclude ("*.bmp")
        exclude ("*.xml")
    }
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/${base.archivesBaseName}-$version")
}

tasks.register<Copy>("copyRuntimeLibs"){
    from (configurations.runtimeClasspath)
            include ("*.jar")
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/${base.archivesBaseName}-$version/lib")
}

tasks.register<Copy>("copyMapserver"){
    println("${rootProject.projectDir}")
    from ("${rootProject.projectDir}/bin") {
        include ("gdal-2-4-0/**")
    }
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/${base.archivesBaseName}-$version/bin")
}

tasks.register<Copy>("copyNatives"){
    from ("${rootProject.projectDir}/bin")
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/${base.archivesBaseName}-$version/bin")
}

tasks.build{finalizedBy("copyJar")}
tasks.build{finalizedBy("copyResources")}
tasks.build{finalizedBy("copyRuntimeLibs")}
tasks.build{finalizedBy("copyMapserver")}
tasks.build{finalizedBy("copyNatives")}

application {
    mainClassName = "importer.MetDataImportWizard"
    applicationDefaultJvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal-2-4-0/bin/gdal/java")
}

tasks.named<JavaExec>("run"){
    environment = mapOf("PATH" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin/gdal/plugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal-2-4-0/bin/proj/SHARE")
}

tasks.getByName("startScripts").enabled = false
tasks.getByName("installDist").enabled = false
tasks.getByName("distZip").enabled = false
tasks.getByName("distTar").enabled = false
