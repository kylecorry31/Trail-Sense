import java.time.LocalDate

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.kylecorry.trail_sense"
    compileSdk = 34

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.kylecorry.trail_sense"
        minSdk = 23
        targetSdk = 34
        versionCode = 112
        versionName = "5.7.0"
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
    }
    buildTypes {
        // Release build (Google Play / F-Droid)
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Staging build (a release build with a ID)
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
        // Debug build (GitHub)
        create("dev") {
            initWith(getByName("debug"))
            signingConfig = signingConfigs.getByName("dev")
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    val navVersion = "2.7.6"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    val roomVersion = "2.6.1"
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    val lifecycleVersion = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Material
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Andromeda
    val andromedaVersion = "81d7c0b694"
    implementation("com.github.kylecorry31.andromeda:core:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:fragments:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:forms:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:csv:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:background:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:camera:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:gpx:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:sound:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:sense:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:signal:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:preferences:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:permissions:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:canvas:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:files:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:notify:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:alerts:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:pickers:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:list:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:qr:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:markdown:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:camera:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:clipboard:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:buzz:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:torch:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:battery:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:compression:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:pdf:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:exceptions:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:print:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:list:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:views:$andromedaVersion")

    // Misc
    implementation("com.github.kylecorry31:subsampling-scale-image-view:3.11.9")
    implementation("com.github.kylecorry31:sol:9.1.1")
    implementation("com.github.kylecorry31:luna:6a88851e2b")
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation("org.junit.platform:junit-platform-runner:1.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}
