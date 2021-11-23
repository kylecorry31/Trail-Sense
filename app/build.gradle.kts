plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {
    compileSdk = 31

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.kylecorry.trail_sense"
        minSdk = 23
        targetSdk = 30
        versionCode = 62
        versionName = "3.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("beta") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = " (Beta)"
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
    packagingOptions {
        resources.merges += "META-INF/LICENSE.md"
        resources.merges += "META-INF/LICENSE-notice.md"
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    lint {
        isAbortOnError = false
    }
}

dependencies {
    kapt("androidx.room:room-compiler:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.31")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.room:room-runtime:2.3.0")
    implementation("androidx.room:room-ktx:2.3.0")
    implementation("androidx.lifecycle:lifecycle-service:2.4.0")
    val cameraxVersion = "1.0.1"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:1.0.0-alpha31")
    implementation("com.google.android.material:material:1.4.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    // Sol
    implementation("com.github.kylecorry31:sol:5.5.0-beta04")

    // Andromeda
    val andromedaVersion = "2.5.0-beta02"
    implementation("com.github.kylecorry31.andromeda:core:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:fragments:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:forms:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:csv:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:jobs:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:location:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:camera:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:gpx:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:json:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:sound:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:sense:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:signal:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:preferences:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:permissions:$andromedaVersion")
    implementation("com.github.kylecorry31.andromeda:services:$andromedaVersion")
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

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    testImplementation("org.junit.platform:junit-platform-runner:1.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
}
