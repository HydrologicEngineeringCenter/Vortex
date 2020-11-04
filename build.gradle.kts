import org.gradle.internal.os.OperatingSystem

plugins {
    java
    id ("org.openjfx.javafxplugin") version "0.0.8"
    id("nebula.release") version "13.1.1"
}

val windows_x64 by configurations.creating
val linux_x64 by configurations.creating
val macOS_x64 by configurations.creating

repositories {
    maven(url = "https://www.hec.usace.army.mil/nexus/repository/maven-public/")
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    mavenCentral()
}

dependencies {
    implementation(project(":vortex-api"))
    implementation("com.google.inject:guice:5.0.0-BETA-1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
    windows_x64 ("net.adoptopenjdk:jre:11.0.6_10@zip")
    windows_x64 ("mil.army.usace.hec:javaHeclib:7-HK@zip")
    windows_x64 ("org.gdal:gdal:2.4.4-win-x64@zip")
    linux_x64("net.adoptopenjdk:jre:11.0.9_10-linux64@tar.gz")
    linux_x64("mil.army.usace.hec:javaHeclib:7-HK-Linux64@tar.gz")
    macOS_x64("net.adoptopenjdk:jre:11.0.7-macOS@zip")
    macOS_x64("mil.army.usace.hec:javaHeclib:7-HK-macOS@zip")
    macOS_x64("org.gdal:gdal:2.4.4-macOSx@zip")
}

javafx {
    version = "12"
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.register<Copy>("copyRuntimeLibs") {
    from(configurations.runtimeClasspath)
    include("*.jar")
    into("$buildDir/distributions/${rootProject.name}-${project.version}/lib")
}

tasks.register<Copy>("copyJavafx"){
    from ("$buildDir/distributions/${base.archivesBaseName}-${project.version}/lib")
    include ("javafx*.jar")
    into ("$buildDir/distributions/${base.archivesBaseName}-${project.version}/jmods")
}

tasks.register<Delete>("deleteJavafx"){
    delete(fileTree("$buildDir/distributions/${base.archivesBaseName}-${project.version}/lib").matching {
        include("javafx*.jar")
    })
}

tasks.register<Copy>("copyImporter") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":importer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":importer").projectDir.toString()
                    + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":importer").projectDir.toString()
                    + "/package/linux")
            include("*.sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":importer").projectDir.toString()
                    + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyImporter").dependsOn(":importer:build")

tasks.register<Copy>("copyNormalizer") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":normalizer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":normalizer").projectDir.toString() + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":normalizer").projectDir.toString() + "/package/linux")
            include("*sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":normalizer").projectDir.toString()
                    + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyNormalizer").dependsOn(":normalizer:build")

tasks.register<Copy>("copyShifter") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":time-shifter").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":time-shifter").projectDir.toString() + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":time-shifter").projectDir.toString() + "/package/linux")
            include("*sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":time-shifter").projectDir.toString() + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyShifter").dependsOn(":time-shifter:build")

tasks.register<Copy>("copyGridToPointConverter") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":grid-to-point-converter").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":grid-to-point-converter").projectDir.toString() + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":grid-to-point-converter").projectDir.toString() + "/package/linux")
            include("*sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":grid-to-point-converter").projectDir.toString() + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyGridToPointConverter").dependsOn(":grid-to-point-converter:build")

tasks.register<Copy>("copyTransposer") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":transposer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":transposer").projectDir.toString() + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":transposer").projectDir.toString() + "/package/linux")
            include("*sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":transposer").projectDir.toString() + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyTransposer").dependsOn(":transposer:build")

tasks.register<Copy>("copySanitizer") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":sanitizer").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":sanitizer").projectDir.toString() + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":sanitizer").projectDir.toString() + "/package/linux")
            include("*sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":sanitizer").projectDir.toString() + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copySanitizer").dependsOn(":sanitizer:build")

tasks.register<Copy>("copyClipper") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":clipper").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":clipper").projectDir.toString() + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":clipper").projectDir.toString() + "/package/linux")
            include("*sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":clipper").projectDir.toString() + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyClipper").dependsOn(":clipper:build")

tasks.register<Copy>("copyImageExporter") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":image-exporter").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin"){
        if(OperatingSystem.current().isWindows()) {
            from(project(":image-exporter").projectDir.toString() + "/package/windows")
            include("*.bat", "*.exe")
        }
        else if(OperatingSystem.current().isLinux()) {
            from(project(":image-exporter").projectDir.toString() + "/package/linux")
            include("*sh")
        }
        else if(OperatingSystem.current().isMacOsX()) {
            from(project(":image-exporter").projectDir.toString() + "/package/macOS")
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyImageExporter").dependsOn(":image-exporter:build")

tasks.register<Copy>("copyLicense") {
    from(project.rootDir) {
        include("LICENSE.md")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-${project.version}")
}

tasks.register<Copy>("copyFatJar") {
    from(project(":vortex-api").buildDir.toString()
            + "/libs") {
        include("${rootProject.name}-all-${project.version}")
    }
    into("${rootProject.projectDir}/build/distributions")
}

tasks.register<Copy>("copyStartScripts") {
    if(OperatingSystem.current().isLinux()) {
        from("$projectDir/package/linux")
        into("$buildDir/distributions/${rootProject.name}-${project.version}/bin")
    }
    else if(OperatingSystem.current().isMacOsX()) {
        from("$projectDir/package/macOS")
        into("$buildDir/distributions/${rootProject.name}-${project.version}/bin")
    }
}

tasks.register<Copy>("getNatives") {
    if(OperatingSystem.current().isWindows()) {
        configurations.getByName("windows_x64").asFileTree.forEach() {
            from(zipTree(it))
            into("$projectDir/bin")
        }
    }
    else if(OperatingSystem.current().isLinux()) {
        configurations.getByName("linux_x64").asFileTree.forEach() {
            from(tarTree(it))
            into("$projectDir/bin")
        }
    }
    else if(OperatingSystem.current().isMacOsX()) {
        configurations.getByName("macOS_x64").asFileTree.forEach() {
            from(zipTree(it))
            into("$projectDir/bin")
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

tasks.register<Copy>("copyNatives") {
    from ("$projectDir/bin")
    exclude ("jre")
    into ("$buildDir/distributions/${rootProject.name}-${project.version}/bin")
}

tasks.getByName("copyNatives") { dependsOn("refreshNatives") }

tasks.register<Copy>("copyJre") {
    from ("$projectDir/bin/jre")
    into ("$buildDir/distributions/${rootProject.name}-${project.version}/jre")
}

tasks.register<Tar>("zipLinux") {
    archiveFileName.set("${rootProject.name}-${project.version}-linux-x64" + ".tar.gz")
    destinationDirectory.set(file("$buildDir/distributions"))
    from (fileTree("$buildDir/distributions/${rootProject.name}-${project.version}"))
    into ("${rootProject.name}-${project.version}")
    compression = Compression.GZIP
}

tasks.register<Zip>("zipWin") {
    archiveFileName.set("${rootProject.name}-${project.version}-win-x64" + ".zip")
    destinationDirectory.set(file("$buildDir/distributions"))
    from (fileTree("$buildDir/distributions/${rootProject.name}-${project.version}"))
    into ("${rootProject.name}-${project.version}")
}

tasks.register("zip") {
    if(OperatingSystem.current().isWindows()) {
        dependsOn("zipWin")
    }
    else if(OperatingSystem.current().isLinux()) {
        dependsOn("zipLinux")
    }
}

tasks.getByName("copyJre").dependsOn("refreshNatives")

tasks.getByName("build") { finalizedBy("copyJre") }
tasks.getByName("build") { finalizedBy("copyRuntimeLibs") }
tasks.getByName("copyRuntimeLibs") { finalizedBy("copyJavafx") }
tasks.getByName("copyJavafx") { finalizedBy("deleteJavafx") }
tasks.getByName("build") { finalizedBy("copyNatives") }
tasks.getByName("build") { finalizedBy("copyImporter") }
tasks.getByName("build") { finalizedBy("copyNormalizer") }
tasks.getByName("build") { finalizedBy("copyShifter") }
tasks.getByName("build") { finalizedBy("copyGridToPointConverter") }
tasks.getByName("build") { finalizedBy("copyTransposer") }
tasks.getByName("build") { finalizedBy("copySanitizer") }
tasks.getByName("build") { finalizedBy("copyClipper") }
tasks.getByName("build") { finalizedBy("copyImageExporter") }
tasks.getByName("build") { finalizedBy("copyLicense") }
tasks.getByName("build") { finalizedBy("copyStartScripts") }
tasks.getByName("build").dependsOn("vortex-api:fatJar")
tasks.getByName("build") { finalizedBy("copyFatJar") }
tasks.getByName("build") { finalizedBy("zip") }
tasks.getByName("zip").dependsOn("copyJre", "copyRuntimeLibs", "copyJavafx", "copyNatives", "copyNatives",
        "copyImporter", "copyNormalizer", "copyShifter", "copyGridToPointConverter", "copyTransposer", "copySanitizer",
        "copyClipper", "copyImageExporter", "copyLicense", "copyStartScripts")

tasks.getByName("final").dependsOn(":build")
tasks.getByName("final").dependsOn("vortex-api:publish")
tasks.getByName("candidate").dependsOn(":build")

tasks.getByName("jar").enabled = false

tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    maxHeapSize = "2g"
}
