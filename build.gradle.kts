plugins {
    base
    id("nebula.release") version "10.1.1"
}

val version = project.version.toString()

tasks.register<Copy>("copyImporter") {
    from(project(":importer").projectDir.toString()
            + "/build/distributions/importer-$version")
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-$version")
}
tasks.getByPath(":copyImporter").dependsOn(":importer:build")

tasks.register<Copy>("copyNormalizer") {
    from(project(":normalizer").projectDir.toString()
            + "/build/distributions/normalizer-$version"){
        exclude ("bin/**")
        exclude ("lib/**")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-$version")
}
tasks.getByPath(":copyNormalizer").dependsOn(":normalizer:build")

tasks.register<Copy>("copyShifter") {
    from(project(":time-shifter").projectDir.toString()
            + "/build/distributions/time-shifter-$version"){
        exclude ("bin/**")
        exclude ("lib/**")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-$version")
}
tasks.getByPath(":copyShifter").dependsOn(":time-shifter:build")

tasks.register<Copy>("copyGridToPointConverter") {
    from(project(":grid-to-point-converter").projectDir.toString()
            + "/build/distributions/grid-to-point-converter-$version"){
        exclude ("bin/**")
        exclude ("lib/**")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-$version")
}
tasks.getByPath(":copyGridToPointConverter").dependsOn(":grid-to-point-converter:build")

tasks.register<Copy>("copyTransposer") {
    from(project(":transposer").projectDir.toString()
            + "/build/distributions/transposer-$version"){
        exclude ("bin/**")
        exclude ("lib/**")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-$version")
}
tasks.getByPath(":copyTransposer").dependsOn(":transposer:build")

tasks.register<Copy>("copyLicense") {
    from(project.rootDir){
        include ("LICENSE.md")
    }
    into("${rootProject.projectDir}/build/distributions/${rootProject.name}-$version")
}

tasks.register<Copy>("copyFatJar") {
    from(project(":vortex-api").buildDir.toString()
            + "/libs"){
        include ("${rootProject.name}-all-$version")
    }
    into("${rootProject.projectDir}/build/distributions")
}

tasks.getByPath(":build").finalizedBy(":copyImporter")
tasks.getByPath(":build").finalizedBy(":copyNormalizer")
tasks.getByPath(":build").finalizedBy(":copyShifter")
tasks.getByPath(":build").finalizedBy(":copyGridToPointConverter")
tasks.getByPath(":build").finalizedBy(":copyTransposer")
tasks.getByPath(":build").finalizedBy(":copyLicense")
tasks.getByPath(":build").dependsOn("vortex-api:fatJar")
tasks.getByPath(":build").finalizedBy(":copyFatJar")

tasks.getByPath(":final").dependsOn(":build")
