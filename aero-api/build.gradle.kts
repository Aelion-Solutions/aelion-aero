plugins {
    `java-library`
    `maven-publish`
}

description = "Thin Aero fleet bridge API for sibling plugins (no Minecraft deps)"

java {
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "aero-api"
            pom {
                name.set("Aelion Aero API")
                description.set(project.description)
                url.set("https://github.com/Aelion-Solutions/aelion-aero")
                licenses {
                    license {
                        name.set("Proprietary")
                    }
                }
                developers {
                    developer {
                        organization.set("Aelion Solutions")
                    }
                }
                scm {
                    url.set("https://github.com/Aelion-Solutions/aelion-aero")
                    connection.set("scm:git:https://github.com/Aelion-Solutions/aelion-aero.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Aelion-Solutions/aelion-aero")
            credentials {
                username = (findProperty("gpr.user") as String?)
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: "github-actions"
                password = (findProperty("gpr.key") as String?)
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: ""
            }
        }
    }
}
