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
    ":backend",
    ":common",
    ":backend-openapi-api",
    ":backend-openapi-processor",
)
