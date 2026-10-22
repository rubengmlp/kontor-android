pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kontor"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:accounts")
include(":feature:transfer")
include(":feature:profile")
