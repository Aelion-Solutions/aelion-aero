dependencies {
    compileOnly("io.github.waterfallmc:waterfall-api:1.21-R0.3-SNAPSHOT")
    implementation(project(":aero-common"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    filesMatching("bungee.yml") {
        expand(props)
    }
}

tasks.jar {
    dependsOn(configurations.runtimeClasspath)
    dependsOn(":aero-common:jar")
    archiveFileName.set("aero-bungee-${project.version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
