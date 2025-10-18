plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    // Minimal target to keep module valid without external deps
    jvm()

    jvmToolchain(17)

    // No dependencies configured; add targets as needed later
}

