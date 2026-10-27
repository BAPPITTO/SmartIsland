/*
 * Smart Island (2026)
 * Original Copyright (c) 2026 Animesh Gupta — github.com/agupta07505
 * Modifications Copyright (c) 2026 Jericho Foster — github.com/BAPPITTO
 * Licensed under the GNU GPL v3 License
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    // Note: This plugin is harmless even if the :baselineprofile module is disabled in settings.gradle.kts
    alias(libs.plugins.baselineprofile)
}

val signingStoreFile = providers.environmentVariable("SIGNING_STORE_FILE")
val signingStorePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD")
val signingKeyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS")
val signingKeyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD")
val hasReleaseSigning = signingStoreFile.isPresent &&
    signingStorePassword.isPresent &&
    signingKeyAlias.isPresent &&
    signingKeyPassword.isPresent

android {
    namespace = "com.agupta07505.smartisland"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.agupta07505.smartisland"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "7.6.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(signingStoreFile.get())
                storePassword = signingStorePassword.get()
                keyAlias = signingKeyAlias.get()
                keyPassword = signingKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "ObsoleteSdkInt",
            "InternalInsetResource",
            "DiscouragedApi",
            "OldTargetApi",
            "UnusedAttribute",
            "LocalContextResourcesRead",
            "LocalContextGetResourceValueCall",
            "PluralsCandidate",
            "TypographyEllipsis",
            "FrequentlyChangingValue",
            "UnusedResources",
            "IconLauncherShape",
            "MonochromeLauncherIcon",
            "UseKtx",
            "IconXmlAndPng",
            "BatteryLife",
            "ConfigurationScreenWidthHeight",
            "ModifierParameter",
            "MissingTranslation",
            "Typos"
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

baselineProfile {
    saveInSrc = true
    mergeIntoMain = true
    automaticGenerationDuringBuild = false
}

dependencies {
    val composeBom = platform(libs.compose.bom)

    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.profileinstaller)
    
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)

    debugImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.json)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}