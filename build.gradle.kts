import com.android.build.gradle.BaseExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {

    // Configure Java to use our chosen language level. Kotlin will automatically pick this up
    plugins.withType<JavaBasePlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
    }
    
    // Configure Android projects
    pluginManager.withPlugin("com.android.application") { configureAndroidProject() }
    pluginManager.withPlugin("com.android.library") { configureAndroidProject() }
    pluginManager.withPlugin("com.android.test") { configureAndroidProject() }
}

fun Project.configureAndroidProject() {
    extensions.configure<BaseExtension> {
        compileSdkVersion(35)

        defaultConfig {
            minSdk = 21
            targetSdk = 35
        }

        // Can remove this once https://issuetracker.google.com/issues/260059413 is fixed.
        // See https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21

            // https://developer.android.com/studio/write/java8-support
            isCoreLibraryDesugaringEnabled = true
        }
    }

    dependencies {
        // https://developer.android.com/studio/write/java8-support
        "coreLibraryDesugaring"(libs.coreDesugaring)
    }
}

// Remove also build folder in root folder
tasks.register<Delete>("clean") {
    delete.add(rootProject.layout.buildDirectory)
}