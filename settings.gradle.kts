pluginManagement {
    repositories {
        // Needed for Android Gradle Plugin
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // AndroidX artifacts and Compose for Android live in Google Maven
        google()
        mavenCentral()
    }
}

rootProject.name = "madprojects"

include(
    ":kmp",
    ":kmp:app",

    ":kmp:common",
    ":kmp:common:feature-common",

    ":kmp:features",
    ":kmp:features:auth:sdk",
    ":kmp:features:auth:impl",
    ":backend",
    ":common")

project(":kmp:features:auth:sdk").projectDir = file("kmp/features/src/auth/sdk")
project(":kmp:features:auth:impl").projectDir = file("kmp/features/src/auth/impl")
project(":kmp:common:feature-common").projectDir = file("kmp/common/src/feature-common")
