plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("clean") {
    delete(rootProject.layout.buildDirectory)
}
