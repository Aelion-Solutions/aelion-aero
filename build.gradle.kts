plugins {
    java
}

fun sanitizedVersion(raw: String): String = raw.substringBefore('#').trim()

allprojects {
    group = providers.gradleProperty("group").get()
    version = sanitizedVersion(providers.gradleProperty("version").get())
    description = providers.gradleProperty("description").orNull
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Jar>().configureEach {
        archiveBaseName.set(project.name)
    }
}
