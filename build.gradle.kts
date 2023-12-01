plugins {
    id("com.android.application") version "8.1.4" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.15" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
}

task("clean") {
    delete(rootProject.buildDir)
}
