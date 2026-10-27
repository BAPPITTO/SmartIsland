/*
 * Smart Island (2026)
 * Original Copyright (c) 2026 Animesh Gupta — github.com/agupta07505
 * Modifications Copyright (c) 2026 Jericho Foster — github.com/BAPPITTO
 * Licensed under the GNU GPL v3 License
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Smart Island"
include(":app")
// include(":baselineprofile")
