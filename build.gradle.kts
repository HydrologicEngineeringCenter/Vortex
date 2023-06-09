import org.gradle.internal.os.OperatingSystem

plugins {
    java
    id("nebula.release") version "13.1.1"
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2")
    windows_x64("net.adoptopenjdk:jre:11.0.6_10@zip")
    windows_x64("mil.army.usace.hec:javaHeclib:7-IP-win-x86_64@zip")
    windows_x64("org.gdal:gdal:3.2.1-win-x64@zip")
    windows_x64("edu.ucar:netcdf:4.9.2:win-x64@zip")
    linux_x64("net.adoptopenjdk:jre:11.0.9_10-linux64@tar.gz")
    linux_x64("mil.army.usace.hec:javaHeclib:7-IP-linux-x86_64@tar.gz")
    linux_x64("org.gdal:gdal:3.0.4:linux@tar.gz")
    macOS_x64("net.adoptium:jre:11.0.15_10:macOS-x64@zip")
    macOS_x64("org.gdal:gdal:3.5.0_1:macOS-x64@zip")
    macOS_x64("org.gdal:gdal-data:3.5.0_1@zip")
    macOS_x64("org.proj:proj-db:9.0.1@zip")
    macOS_x64("mil.army.usace.hec:javaHeclib:7-IP:macOS-x64@zip")
    macOS_aarch64("net.adoptium:jre:11.0.15_10:macOS-aarch64@zip")
    macOS_aarch64("org.gdal:gdal:3.5.0_1:macOS-aarch64@zip")
    macOS_aarch64("mil.army.usace.hec:javaHeclib:7-IO-RC2:macOS-aarch64@zip")
    macOS_aarch64("org.gdal:gdal-data:3.5.0_1@zip")
    macOS_aarch64("org.proj:proj-db:9.0.1@zip")
}

tasks.register<Copy>("copyRuntimeLibs") {
    from(configurations.runtimeClasspath)
    include("*.jar")
    into("$buildDir/distributions/${rootProject.name}-${project.version}/lib")
}

tasks.register<Copy>("copyVortexUi") {
    into("$buildDir/distributions/${rootProject.name}-${project.version}")
    into("lib") {
        from(project(":vortex-ui").buildDir.toString() + "/libs")
        include("*.jar")
    }
    into("bin") {
        if (OperatingSystem.current().isWindows()) {
            from(
                project(":vortex-ui").projectDir.toString()
                        + "/package/windows"
            )
            include("*.bat", "*.exe")
        } else if (OperatingSystem.current().isLinux()) {
            from(
                project(":vortex-ui").projectDir.toString()
                        + "/package/linux"
            )
            include("*.sh")
        } else if (OperatingSystem.current().isMacOsX()) {
            from(
                project(":vortex-ui").projectDir.toString()
                        + "/package/macOS"
            )
            include("*.sh")
        }
    }
}
tasks.getByPath(":copyVortexUi").dependsOn(":vortex-ui:build")

tasks.register<Copy>("copyLicense") {
    from(project.rootDir) {
        include("LICENSE.md")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-${project.version}")
}

tasks.register<Copy>("copyFatJar") {
    from(
        project(":vortex-api").buildDir.toString()
                + "/libs"
    ) {
        include("${rootProject.name}-all-${project.version}")
    }
    into("${rootProject.projectDir}/build/distributions")
}

tasks.register<Copy>("copyStartScripts") {
    if (OperatingSystem.current().isLinux()) {
        from("$projectDir/package/linux")
        into("$buildDir/distributions/${rootProject.name}-${project.version}/bin")
    } else if (OperatingSystem.current().isMacOsX()) {
        from("$projectDir/package/macOS")
        into("$buildDir/distributions/${rootProject.name}-${project.version}/bin")
    }
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

}

tasks.register<Delete>("refreshNatives") {
    destroyables.register("$projectDir/x64")
    doLast {
        delete("$projectDir/bin")
    }
}
tasks.getByName("refreshNatives") { finalizedBy("getNatives") }

tasks.register<Copy>("copyNatives") {
    from("$projectDir/bin")
    exclude("jre")
    into("$buildDir/distributions/${rootProject.name}-${project.version}/bin")
}

tasks.getByName("copyNatives") { dependsOn("refreshNatives") }

tasks.register<Copy>("copyJre") {
    from("$projectDir/bin/jre")
    into("$buildDir/distributions/${rootProject.name}-${project.version}/jre")
}

tasks.register<Tar>("zipLinux") {
    archiveFileName.set("${rootProject.name}-${project.version}-linux-x64" + ".tar.gz")
    destinationDirectory.set(file("$buildDir/distributions"))
    from(fileTree("$buildDir/distributions/${rootProject.name}-${project.version}"))
    into("${rootProject.name}-${project.version}")
    compression = Compression.GZIP
}

tasks.register<Zip>("zipWin") {
    archiveFileName.set("${rootProject.name}-${project.version}-win-x64" + ".zip")
    destinationDirectory.set(file("$buildDir/distributions"))
    from(fileTree("$buildDir/distributions/${rootProject.name}-${project.version}"))
    into("${rootProject.name}-${project.version}")
}

tasks.register<Zip>("zipMacOS") {
    archiveFileName.set("${rootProject.name}-${project.version}-macOS-x64" + ".zip")
    destinationDirectory.set(file("$buildDir/distributions"))
    from(fileTree("$buildDir/distributions/${rootProject.name}-${project.version}"))
    into("${rootProject.name}-${project.version}")
}

tasks.register("zip") {
    if (OperatingSystem.current().isWindows()) {
        dependsOn("zipWin")
    } else if (OperatingSystem.current().isLinux()) {
        dependsOn("zipLinux")
    } else if (OperatingSystem.current().isMacOsX()) {
        dependsOn("zipMacOS")
    }
}

tasks.getByName("copyJre").dependsOn("refreshNatives")

tasks.getByName("build") { finalizedBy("copyJre") }
tasks.getByName("build") { finalizedBy("copyRuntimeLibs") }
tasks.getByName("build") { finalizedBy("copyNatives") }
tasks.getByName("build") { finalizedBy("copyVortexUi") }
tasks.getByName("build") { finalizedBy("copyLicense") }
tasks.getByName("build") { finalizedBy("copyStartScripts") }
tasks.getByName("build").dependsOn("vortex-api:fatJar")
tasks.getByName("build") { finalizedBy("copyFatJar") }
tasks.getByName("build") { finalizedBy("zip") }
tasks.getByName("zip").dependsOn(
    "copyJre", "copyRuntimeLibs", "copyNatives", "copyVortexUi", "copyLicense", "copyStartScripts"
)

tasks.getByName("final").dependsOn(":build")
tasks.getByName("final").dependsOn("vortex-api:publish")
tasks.getByName("final").dependsOn("vortex-ui:publish")
tasks.getByName("candidate").dependsOn(":build")

tasks.getByName("jar").enabled = false

tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    maxHeapSize = "2g"
}
