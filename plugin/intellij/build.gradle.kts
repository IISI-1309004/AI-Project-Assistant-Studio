plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "1.9.24"
}

group = "com.aipa"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}


intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ IDEA Community
    plugins.set(listOf())
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
    }
}
