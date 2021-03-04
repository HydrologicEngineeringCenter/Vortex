plugins {
    java
    application
    id ("org.openjfx.javafxplugin")
}

base.archivesBaseName = "importer"
val version = project.version.toString()

repositories {
    maven(url = "https://www.hec.usace.army.mil/nexus/repository/maven-public/")
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    mavenCentral()
}

dependencies {
    implementation(project(":vortex-api"))
    implementation("org.slf4j:slf4j-simple:1.7.25")
    implementation("com.google.inject:guice:5.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
}

javafx {
    version = "12"
    modules = listOf("javafx.controls", "javafx.fxml")
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
    into ("$buildDir/distributions/${base.archivesBaseName}-$version")
}

tasks.register<Copy>("copyResources"){
    from ("$projectDir/src/main/deploy/package/windows"){
        exclude ("*.ico")
        exclude ("*.bmp")
        exclude ("*.xml")
    }
    into ("$buildDir/distributions/${base.archivesBaseName}-$version")
}

tasks.register<Copy>("copyRuntimeLibs"){
    from (configurations.runtimeClasspath)
            include ("*.jar")
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/lib")
}

tasks.register<Copy>("copyJavafx"){
    from ("$buildDir/distributions/${base.archivesBaseName}-$version/lib")
    include ("javafx*.jar")
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/jmods")
}

tasks.register<Delete>("deleteJavafx"){
    delete(fileTree("$buildDir/distributions/${base.archivesBaseName}-$version/lib").matching {
        include("javafx*.jar")
    })
}

tasks.register<Copy>("copyNatives"){
    from ("${rootProject.projectDir}/bin")
    into ("$buildDir/distributions/${base.archivesBaseName}-$version/bin")
}

tasks.build{finalizedBy("copyJar")}
tasks.build{finalizedBy("copyResources")}
tasks.build{finalizedBy("copyRuntimeLibs")}
tasks.getByName("copyRuntimeLibs") { finalizedBy("copyJavafx") }
tasks.getByName("copyJavafx") { finalizedBy("deleteJavafx") }
tasks.build{finalizedBy("copyNatives")}

application {
    mainClassName = "importer.MetDataImportWizard"
    applicationDefaultJvmArgs = listOf("-Djava.library.path=${rootProject.projectDir}/bin;${rootProject.projectDir}/bin/gdal")
}

tasks.named<JavaExec>("run"){
    environment = mapOf("PATH" to "${rootProject.projectDir}/bin/gdal",
            "GDAL_DRIVER_PATH" to "${rootProject.projectDir}/bin/gdal/gdalplugins",
            "GDAL_DATA" to "${rootProject.projectDir}/bin/gdal/gdal-data",
            "PROJ_LIB" to "${rootProject.projectDir}/bin/gdal/projlib")
}

tasks.getByName("startScripts").enabled = false
tasks.getByName("installDist").enabled = false
tasks.getByName("distZip").enabled = false
tasks.getByName("distTar").enabled = false
