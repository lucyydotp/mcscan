import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "net.lucypoulton"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("com.github.ajalt.clikt:clikt:3.2.0")
    implementation("de.m3y.kformat:kformat:0.8")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    withType<ShadowJar> {
        archiveClassifier.set("")
    }

    jar {
        manifest {
            attributes("Main-Class" to "net.lucypoulton.mcscan.MainKt")
        }
        archiveClassifier.set("nodeps")
    }
}

tasks["build"].dependsOn(tasks["shadowJar"])