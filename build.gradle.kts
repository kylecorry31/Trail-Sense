buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath(kotlin("gradle-plugin", version = "1.7.10"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven{
            url = uri("https://jitpack.io")
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
    }
}

task("clean") {
    delete(rootProject.buildDir)
}
