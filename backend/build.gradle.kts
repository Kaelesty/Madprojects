plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

// Use repositories defined in settings.gradle.kts

dependencies {
    testImplementation(libs.kotlin.test)

    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.network.tls.certificates)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.serialization.jackson)

    implementation(libs.javax.mail)
    implementation(libs.poi)
    implementation(libs.poi.ooxml)

    implementation(project(":common"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("app.MainKt")
}

// Convenience task to build and run backend in one go
tasks.register<JavaExec>("runBackend") {
    group = "application"
    description = "Builds and runs the backend module"
    dependsOn("build")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.MainKt")
}

ktor {
    docker {
        localImageName.set("kaelesty/ktor-docker-image")
        imageTag.set("release")
        jreVersion.set(JavaVersion.VERSION_17)
    }
}

tasks.named("jibBuildTar") {
    dependsOn("classes", "processResources")
}
