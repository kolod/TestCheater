import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    application
    kotlin("jvm") version "2.2.0"
    kotlin("kapt") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.8"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.0")
    implementation("org.slf4j:slf4j-log4j12:2.0.17")
    implementation("org.apache.logging.log4j:log4j-core:2.25.1")
    implementation("org.apache.logging.log4j:log4j-api:2.25.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.25.1")
    implementation("io.github.kolod:flatlaf-themes-combobox-model:1.1.1")
    implementation("com.formdev:flatlaf:3.6.1")
    implementation("com.formdev:flatlaf-extras:3.6.1")
    implementation("com.formdev:flatlaf-intellij-themes:3.6.1")
    implementation("org.drjekyll:fontchooser:3.1.0")
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")
    implementation("com.jcabi:jcabi-manifests:2.1.0")
}

application {
    mainClass.set(project.ext["mainClassName"] as String)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
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
