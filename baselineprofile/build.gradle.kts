/*
 * Smart Island (2026)
 * Original Copyright (c) 2026 Animesh Gupta — github.com/agupta07505
 * Modifications Copyright (c) 2026 Jericho Foster — github.com/BAPPITTO
 * Licensed under the GNU GPL v3 License
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.agupta07505.smartisland.baselineprofile"
    compileSdk = 36

    defaultConfig {
        // Generating a profile needs API 28+ (33+ on a phone that is not rooted).
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    lint {
        // The test module is not shipped; keep it out of the app's warningsAsErrors run.
        abortOnError = false
        warningsAsErrors = false
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
}
