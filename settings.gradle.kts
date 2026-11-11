pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                val localProperties = java.util.Properties().apply {
                    val localFile = java.io.File(settingsDir, "local.properties")
                    if (localFile.exists()) {
                        localFile.inputStream().use(::load)
                    }
                }
                username = "mapbox" // Este nome de usuário é fixo para Mapbox
                password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
                    ?: providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull
                    ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven { url = uri("https://dev.dji.com/repo/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.mapbox.com/downloads/v2/releases/maven") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "RegisterApp"
include(":app")
