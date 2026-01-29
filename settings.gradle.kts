pluginManagement {
    repositories {
        google()  // Required for Firebase & Android plugins
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // safer for multi-module projects

    repositories {
        google() // Firebase artifacts are here
        mavenCentral()
        maven("https://jitpack.io") // required for osmbonuspack
    }
}

rootProject.name = "NagarFix"
include(":app")
