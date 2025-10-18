plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.android.application)
}

kotlin {
    androidTarget() {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)

                implementation(libs.jetbrains.androidx.navigation.compose)
                implementation(libs.koin.core)

                implementation(project(":kmp:common:feature-common"))
                implementation(project(":kmp:features:auth:sdk"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
        // iOS source sets are created via the default hierarchy
    }
}

android {
    namespace = "ru.kaelesty.madprojects"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.kaelesty.madprojects"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    add("commonMainImplementation", platform("io.insert-koin:koin-bom:4.0.0"))
    add("androidMainImplementation", platform("io.insert-koin:koin-bom:4.0.0"))
    implementation(libs.androidx.navigation.compose.jvmstubs)
}
