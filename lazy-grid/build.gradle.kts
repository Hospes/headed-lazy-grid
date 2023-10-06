plugins {
    `maven-publish`
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ua.hospes.lazygrid"

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composecompiler.get()
    }

    kotlinOptions {
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.Hospes"
            artifactId = "headed-lazy-grid"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.foundation)
}