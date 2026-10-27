/*
 * Smart Island (2026)
 * Original Copyright (c) 2026 Animesh Gupta — github.com/agupta07505
 * Modifications Copyright (c) 2026 Jericho Foster — github.com/BAPPITTO
 * Licensed under the GNU GPL v3 License
 */

plugins {
    alias(libs.plugins.android.library)
    // CRITICAL: Required to resolve the kotlin { } and compilerOptions blocks 
    // when android.builtInKotlin=false is active in gradle.properties.
    alias(libs.plugins.kotlin.android)
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Legacy Kotlin options block (compatible with the disabled new DSL)
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
    // FIXED: This must be inside the baselineProfile block, not android {}
    targetProjectPath = ":app"
    useConnectedDevices = true
}

dependencies {
    // Latest stable versions verified for late 2026 compatibility
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
}
