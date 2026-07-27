pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

rootProject.name = "aelion-aero"

include("aero-api")
include("aero-common")
include("aero-bukkit-shared")
include("aero-bukkit-1_8")
include("aero-bukkit-1_13")
include("aero-paper-1_17")
include("aero-paper-1_21")
include("aero-paper-26")
include("aero-velocity")
include("aero-bungee")
