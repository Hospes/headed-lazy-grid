@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex(".*google.*")
                includeGroupByRegex(".*android.*")
            }
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex(".*google.*")
                includeGroupByRegex(".*android.*")
            }
        }
        mavenCentral()
    }
}

//enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// https://docs.gradle.org/7.6/userguide/configuration_cache.html#config_cache:stable
//enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "LazyGrid"

include(
    ":lazy-grid",
    ":sample",
)