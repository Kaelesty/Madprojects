plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

val kmpBaseUrlProvider = providers.gradleProperty("kmp.baseUrl")
    .orElse("http://localhost:8080")
val generatedKtorConfigDir = layout.buildDirectory.dir("generated/kmpConfig")

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
            kotlin.srcDir(generatedKtorConfigDir)
            dependencies {
                implementation(project(":common"))
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.components.resources)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)

                implementation(libs.jetbrains.androidx.navigation.compose)
                implementation(libs.koin.core)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
                implementation(libs.kermit)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.cio)
                implementation(libs.androidx.security.crypto)
                implementation(libs.koin.android)
            }
        }

        // Define a shared iOS source set for the legacy hierarchy.
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "ru.kaelesty.madprojects"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.kaelesty.madprojects"
        minSdk = 24
        targetSdk = 35
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
}

val generateKtorConfig by tasks.registering {
    outputs.dir(generatedKtorConfigDir)
    doLast {
        val baseUrl = kmpBaseUrlProvider.get()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        val outputFile = generatedKtorConfigDir.get()
            .file("ru/kaelesty/madprojects/ktor/KtorConfig.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package ru.kaelesty.madprojects.ktor

            object KtorConfig {
                const val BaseUrl = "$baseUrl"
            }
            """.trimIndent()
        )
    }
}

tasks.configureEach {
    if (name.startsWith("compile") && name.contains("Kotlin")) {
        dependsOn(generateKtorConfig)
    }
}

tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach {
    dependsOn(generateKtorConfig)
}
