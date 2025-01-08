plugins {
    kotlin("jvm") version (libs.versions.kotlin.get())
    alias(libs.plugins.pv.entrypoints)
    alias(libs.plugins.pv.java.templates)
    alias(libs.plugins.pv.kotlin.relocate)
    `maven-publish`
}

if (properties.containsKey("snapshot")) {
    version = "$version-SNAPSHOT"
}

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://repo.plasmoverse.com/releases")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(rootProject.libs.kotlinx.coroutines.core)
    compileOnly(rootProject.libs.kotlinx.coroutines.jdk8)

    compileOnly(libs.pv.proxy)
}

tasks {
    jar {
        enabled = false
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    }

    shadowJar {
        archiveClassifier.set("")
    }
}
