import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    application
    kotlin("jvm") version "1.9.20"
    kotlin("kapt") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

project.ext {
    set("mainClassName", "io.github.kolod.TestCheater")
}

version = SimpleDateFormat("yy.M.d").format(Date())

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
    implementation("org.slf4j:slf4j-log4j12:2.0.12")
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.22.1")
    implementation("io.github.kolod:flatlaf-themes-combobox-model:1.1.1")
    implementation("com.formdev:flatlaf:3.3")
    implementation("com.formdev:flatlaf-extras:3.3")
    implementation("com.formdev:flatlaf-intellij-themes:3.3")
    implementation("org.drjekyll:fontchooser:2.5.2")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("com.jcabi:jcabi-manifests:2.1.0")
}

application {
    mainClass.set(project.ext["mainClassName"] as String)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

tasks.named<ShadowJar>("shadowJar") {
    manifest {
        attributes["Implementation-Title"] = "TestCheater"
        attributes["Main-Class"] = project.ext["mainClassName"] as String
        attributes["Author"] = "Oleksandr Kolodkin"
        attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
        attributes["Multi-Release"] = "true"
        attributes["Build-Date"] = SimpleDateFormat("dd/MM/yyyy").format(Date())
    }
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Implementation-Title"] = "TestCheater"
        attributes["Main-Class"] = project.ext["mainClassName"] as String
        attributes["Author"] = "Oleksandr Kolodkin"
        attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
        attributes["Multi-Release"] = "true"
        attributes["Build-Date"] = SimpleDateFormat("dd/MM/yyyy").format(Date())
    }
    // here zip stuff found in runtimeClasspath:
    from(configurations.runtimeClasspath.get().map {if (it.isDirectory) it else zipTree(it)})
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
