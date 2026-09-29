pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
    }
}

rootProject.name = "apothic-attributes-fabric"

// Placebo is Apothic Attributes' required base library, ported separately (same as for Apotheosis).
includeBuild("../placebo-fabric")
