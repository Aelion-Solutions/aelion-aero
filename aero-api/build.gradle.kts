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

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        encoding = "UTF-8"
        charSet = "UTF-8"
        memberLevel = JavadocMemberLevel.PUBLIC
        windowTitle = "Aelion Aero API"
        docTitle = "Aelion Aero API"
        addBooleanOption("Xdoclint:all,-missing", true)
    }
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
            name = "AelionMaven"
            url = uri(
                if (version.toString().endsWith("-SNAPSHOT")) {
                    "https://maven.aelion.solutions/snapshots"
                } else {
                    "https://maven.aelion.solutions/releases"
                }
            )
            credentials {
                username = (findProperty("aelionMaven.user") as String?)
                    ?: System.getenv("AELION_MAVEN_USER")
                    ?: ""
                password = (findProperty("aelionMaven.token") as String?)
                    ?: System.getenv("AELION_MAVEN_TOKEN")
                    ?: ""
            }
        }
    }
}
