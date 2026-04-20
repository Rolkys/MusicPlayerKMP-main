// FILE: composeApp/build.gradle.kts
import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinComposeCompiler)
    alias(libs.plugins.androidApplication) apply false
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
    apply(plugin = "com.android.application")
}

kotlin {
    if (hasAndroidSdk) {
        androidTarget()
    }
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
        if (hasAndroidSdk) {
            val androidMain by getting {
                dependencies {
                    implementation(compose.preview)
                    implementation(libs.androidxActivityCompose)
                    implementation(libs.androidxCoreKtx)
                }
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

if (hasAndroidSdk) {
    extensions.configure<ApplicationExtension> {
        namespace = "com.example.musicplayer"
        compileSdk = 34

        defaultConfig {
            applicationId = "com.example.musicplayer"
            minSdk = 24
            targetSdk = 34
            versionCode = 1
            versionName = "1.0"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs("-Dskiko.renderApi=OPENGL")
    }
}
