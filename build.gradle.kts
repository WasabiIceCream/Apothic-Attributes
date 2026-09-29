plugins {
    id("fabric-loom") version "1.17.20"
    `java-library`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // javax.annotation.Nullable etc. (NeoForge bundles it transitively, Fabric doesn't).
    include("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // Placebo, Apothic Attributes' required base library (composite build, see settings.gradle.kts).
    implementation("dev.shadowsoffire.placebo:placebo-fabric")
}

loom {
    accessWidenerPath.set(file("src/main/resources/apothic_attributes.accesswidener"))
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
