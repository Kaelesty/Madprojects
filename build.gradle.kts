plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.compose) apply false
}

allprojects {
    group = "ru.kaelesty.madprojects"
    version = "2.0.0"
}

// Repositories are managed centrally in settings.gradle.kts via
// dependencyResolutionManagement to comply with RepositoriesMode.

// Root-level convenience task to run backend
tasks.register("runBackend") {
    group = "application"
    description = "Builds and runs the :backend module"
    dependsOn(":backend:runBackend")
}

// Configure Gradle Wrapper to use 8.10.2 when generated
tasks.wrapper {
    gradleVersion = "8.10.2"
    distributionType = Wrapper.DistributionType.BIN
}
