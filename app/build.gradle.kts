import java.time.LocalDate

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.kylecorry.trail_sense"
    compileSdk = 36

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.kylecorry.trail_sense"
        minSdk = 23
        targetSdk = 36
        versionCode = 133
        versionName = "7.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("nightly") {
            if (System.getProperty("nightly_store_file") != null) {
                storeFile = file(System.getProperty("nightly_store_file"))
                storePassword = System.getProperty("nightly_store_password")
                keyAlias = System.getProperty("nightly_key_alias")
                keyPassword = System.getProperty("nightly_key_password")
            }
        }
        create("dev") {
            if (System.getProperty("dev_store_file") != null) {
                storeFile = file(System.getProperty("dev_store_file"))
                storePassword = System.getProperty("dev_store_password")
                keyAlias = System.getProperty("dev_key_alias")
                keyPassword = System.getProperty("dev_key_password")
            }
        }
    }
    androidResources {
        // Support for auto-generated locales for per-app language settings
        generateLocaleConfig = true
    }
    buildFeatures {
        // Support for view binding
        viewBinding = true
        buildConfig = true
        aidl = true
    }
    buildTypes {
        // Release build (Google Play / F-Droid)
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug"){
            testProguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Staging build (a release build with a ID)
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
        // Play store build
        create("playStore"){
            initWith(getByName("release"))
        }
        // Debug build (GitHub)
        create("dev") {
            initWith(getByName("debug"))
            signingConfig = signingConfigs.getByName("dev")
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        // Release build (GitHub) - uses the same signing as the dev build
        create("github") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("dev")
            applicationIdSuffix = ".github"
            versionNameSuffix = "-github"
        }
        create("nightly") {
            initWith(getByName("debug"))
            signingConfig = signingConfigs.getByName("nightly")
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-nightly-${LocalDate.now()}"
        }
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources.merges += "META-INF/LICENSE.md"
        resources.merges += "META-INF/LICENSE-notice.md"
        jniLibs {
            useLegacyPackaging = true
        }
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Kotlin
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX
    implementation(libs.constraintlayout)
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.gridlayout)
    implementation(libs.preference.ktx)
    implementation(libs.work.runtime.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.legacy.support.v4)
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.androidx.core.remoteviews)
    implementation(libs.androidx.exifinterface)

    // Material
    implementation(libs.material)
    implementation(libs.flexbox)

    // Andromeda
    implementation(libs.andromeda.core)
    implementation(libs.andromeda.fragments)
    implementation(libs.andromeda.forms)
    implementation(libs.andromeda.csv)
    implementation(libs.andromeda.background)
    implementation(libs.andromeda.camera)
    implementation(libs.andromeda.gpx)
    implementation(libs.andromeda.sound)
    implementation(libs.andromeda.sense)
    implementation(libs.andromeda.signal)
    implementation(libs.andromeda.preferences)
    implementation(libs.andromeda.permissions)
    implementation(libs.andromeda.canvas)
    implementation(libs.andromeda.files)
    implementation(libs.andromeda.notify)
    implementation(libs.andromeda.alerts)
    implementation(libs.andromeda.pickers)
    implementation(libs.andromeda.list)
    implementation(libs.andromeda.qr)
    implementation(libs.andromeda.markdown)
    implementation(libs.andromeda.camera)
    implementation(libs.andromeda.clipboard)
    implementation(libs.andromeda.haptics)
    implementation(libs.andromeda.bitmaps)
    implementation(libs.andromeda.torch)
    implementation(libs.andromeda.battery)
    implementation(libs.andromeda.compression)
    implementation(libs.andromeda.pdf)
    implementation(libs.andromeda.exceptions)
    implementation(libs.andromeda.print)
    implementation(libs.andromeda.list)
    implementation(libs.andromeda.views)
    implementation(libs.andromeda.xml)
    implementation(libs.andromeda.json)
    implementation(libs.andromeda.widgets)

    // Misc
    implementation(libs.sol)
    implementation(libs.luna)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.test.arch.core)
    androidTestImplementation(libs.work.testing)
    debugImplementation(libs.androidx.fragment.testing.manifest)
    testImplementation(libs.junit.platform.runner)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.kotlin)
}
