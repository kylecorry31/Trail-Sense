buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath(kotlin("gradle-plugin", version = "1.7.10"))
    }
}

allprojects {
    repositories {
        maven{
            url = uri("https://jitpack.io")
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
        google()
        mavenCentral()
    }
}

task("clean") {
    delete(rootProject.buildDir)
}
