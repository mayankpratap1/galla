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
        google()        // Required for LiteRT-LM + Compose + Room + Camera
        mavenCentral()  // Required for kotlinllamacpp + Ktor + Coil
    }
}
rootProject.name = "EdgeLLMPro"
include(":app")
