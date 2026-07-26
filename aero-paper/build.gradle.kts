dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation(project(":aero-common"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    dependsOn(configurations.runtimeClasspath)
    dependsOn(":aero-common:jar")
    archiveFileName.set("aero-paper-${project.version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
