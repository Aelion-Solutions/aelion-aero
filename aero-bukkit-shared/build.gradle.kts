plugins {
    `java-library`
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    api(project(":aero-common"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}
