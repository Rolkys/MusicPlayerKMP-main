// FILE: shared/build.gradle.kts
import com.android.build.api.dsl.LibraryExtension
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinComposeCompiler)
    alias(libs.plugins.androidLibrary) apply false
}

val hasAndroidSdk = run {
    val sdkFromEnv = listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")
        .mapNotNull { System.getenv(it) }
        .any { it.isNotBlank() && file(it).exists() }

    val sdkFromLocalProperties = rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { propsFile ->
            val props = Properties()
            propsFile.inputStream().use(props::load)
            props.getProperty("sdk.dir")
        }
        ?.takeIf { it.isNotBlank() }
        ?.let { rootProject.file(it).exists() } == true

    sdkFromEnv || sdkFromLocalProperties
}

if (hasAndroidSdk) {
    apply(plugin = "com.android.library")
}

kotlin {
    if (hasAndroidSdk) {
        androidTarget()
    }
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxDatetime)
                implementation(libs.kotlinxCoroutinesCore)
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation(libs.androidxLifecycleViewmodelCompose)
            }
        }
        if (hasAndroidSdk) {
            val androidMain by getting {
                dependencies {
                    implementation("io.ktor:ktor-client-okhttp:2.3.12")
                }
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("org.openjfx:javafx-base:21.0.2:win")
                implementation("org.openjfx:javafx-graphics:21.0.2:win")
                implementation("org.openjfx:javafx-media:21.0.2:win")
                implementation("org.openjfx:javafx-swing:21.0.2:win")
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
            }
        }
    }
}

if (hasAndroidSdk) {
    extensions.configure<LibraryExtension> {
        namespace = "com.example.shared"
        compileSdk = 34
        defaultConfig { minSdk = 24 }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
