plugins {
    java
    id ("org.openjfx.javafxplugin") version "0.0.8"
    id("nebula.release") version "13.1.1"
}

val version = project.version.toString()

val windows_x64 by configurations.creating

repositories {
    maven(url = "https://www.hec.usace.army.mil/nexus/repository/maven-public/")
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
    windows_x64 ("net.adoptopenjdk:jre:11.0.6_10@zip")
    windows_x64 ("mil.army.usace.hec:javaHeclib:7-HK@zip")
    windows_x64 ("org.gdal:gdal:2.4.4-win-x64@zip")
}

javafx {
    version = "12"
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.register<Copy>("copyRuntimeLibs") {
    from(configurations.runtimeClasspath)
    include("*.jar")
    into("$buildDir/distributions/${rootProject.name}-$version/lib")
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

tasks.register<Copy>("copyImporter") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":importer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":importer").projectDir.toString()
                + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyImporter").dependsOn(":importer:build")

tasks.register<Copy>("copyNormalizer") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":normalizer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":normalizer").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyNormalizer").dependsOn(":normalizer:build")

tasks.register<Copy>("copyShifter") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":time-shifter").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":time-shifter").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyShifter").dependsOn(":time-shifter:build")

tasks.register<Copy>("copyGridToPointConverter") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":grid-to-point-converter").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":grid-to-point-converter").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyGridToPointConverter").dependsOn(":grid-to-point-converter:build")

tasks.register<Copy>("copyTransposer") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":transposer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":transposer").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyTransposer").dependsOn(":transposer:build")

tasks.register<Copy>("copySanitizer") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":sanitizer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":sanitizer").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copySanitizer").dependsOn(":sanitizer:build")

tasks.register<Copy>("copyClipper") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":clipper").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":clipper").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyClipper").dependsOn(":clipper:build")

tasks.register<Copy>("copyImageExporter") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":image-exporter").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":image-exporter").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyImageExporter").dependsOn(":image-exporter:build")

tasks.register<Copy>("copyTravelLengthGridCellsExporter") {
    into("$buildDir/distributions/${rootProject.name}-$version")
    into("lib") {
        from(project(":travel-length-grid-cells-exporter").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        from(project(":travel-length-grid-cells-exporter").projectDir.toString() + "/package/windows")
        include("*.bat", "*.exe")
    }
}
tasks.getByPath(":copyTravelLengthGridCellsExporter").dependsOn(":travel-length-grid-cells-exporter:build")

tasks.register<Copy>("copyLicense") {
    from(project.rootDir) {
        include("LICENSE.md")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-$version")
}

tasks.register<Copy>("copyFatJar") {
    from(project(":vortex-api").buildDir.toString()
            + "/libs") {
        include("${rootProject.name}-all-$version")
    }
    into("${rootProject.projectDir}/build/distributions")
}

tasks.register<Copy>("getNatives") {
    configurations.getByName("windows_x64").asFileTree.forEach() {
        from(zipTree(it))
        into("$projectDir/bin")
    }
}

tasks.register<Delete>("refreshNatives") {
    destroyables.register("$projectDir/x64")
    doLast {
        delete("$projectDir/bin")
    }
}
tasks.getByName("refreshNatives") { finalizedBy("getNatives") }

tasks.register<Copy>("copyNatives") {
    from ("$projectDir/bin")
    exclude ("jre")
    into ("$buildDir/distributions/${rootProject.name}-$version/bin")
}

tasks.getByName("copyNatives") { dependsOn("refreshNatives") }

tasks.register<Copy>("copyJre") {
    from ("$projectDir/bin/jre")
    into ("$buildDir/distributions/${rootProject.name}-$version/jre")
}

tasks.getByName("copyJre") {dependsOn("refreshNatives")}

tasks.getByPath(":build").finalizedBy(":copyJre")
tasks.getByPath(":build").finalizedBy(":copyRuntimeLibs")
tasks.getByName("copyRuntimeLibs") { finalizedBy("copyJavafx") }
tasks.getByName("copyJavafx") { finalizedBy("deleteJavafx") }
tasks.getByPath(":build").finalizedBy(":copyNatives")
tasks.getByPath(":build").finalizedBy(":copyImporter")
tasks.getByPath(":build").finalizedBy(":copyNormalizer")
tasks.getByPath(":build").finalizedBy(":copyShifter")
tasks.getByPath(":build").finalizedBy(":copyGridToPointConverter")
tasks.getByPath(":build").finalizedBy(":copyTransposer")
tasks.getByPath(":build").finalizedBy(":copySanitizer")
tasks.getByPath(":build").finalizedBy(":copyClipper")
tasks.getByPath(":build").finalizedBy(":copyImageExporter")
tasks.getByPath(":build").finalizedBy(":copyTravelLengthGridCellsExporter")
tasks.getByPath(":build").finalizedBy(":copyLicense")

tasks.getByPath(":build").dependsOn("vortex-api:fatJar")
tasks.getByPath(":build").finalizedBy(":copyFatJar")

tasks.getByPath(":final").dependsOn(":build")
tasks.getByName("candidate").dependsOn(":build")

tasks.getByPath(":jar").enabled = false

tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    maxHeapSize = "2g"
}
