plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    // Minimal target to keep module valid without external deps
    jvm()

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.jetbrains.androidx.navigation.compose)
                implementation(libs.koin.core)

                implementation(project(":kmp:common:feature-common"))
                implementation(project(":kmp:features:auth:impl"))
            }
        }
    }
}
