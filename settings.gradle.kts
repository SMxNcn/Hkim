rootProject.name = "Hkim"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://jitpack.io")
    }

    val loomVersion = providers.gradleProperty("loom_version").get()
    val kotlinVersion = providers.gradleProperty("kotlin_version").get()

    plugins {
        id("net.fabricmc.fabric-loom") version loomVersion
        kotlin("jvm") version kotlinVersion
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10-alpha.12"
}

stonecutter {
    create(rootProject) {
        version("26.2")
        version("26.3")
        vcsVersion = "26.2"
    }
}
