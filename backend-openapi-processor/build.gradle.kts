plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":backend-openapi-api"))
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
