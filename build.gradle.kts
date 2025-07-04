// Top-level build file where you can add configuration options common to all sub-modules/projects.
buildscript {
    repositories {
        google()  // Required for Android Gradle Plugin
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
}
