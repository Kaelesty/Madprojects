plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.20" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.compose") version "1.7.0" apply false
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
