import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    application
    kotlin(Kotlin.jvmId) version Kotlin.version
    kotlin(Kotlin.kaptId) version Kotlin.version
    shadow(Shadow.id) version Shadow.version
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
    implementation(Kotlin.stdlibJdk8)
    implementation(Slf4j.core)
    implementation(Log4j.core)
    implementation(Log4j.api)
    implementation(Log4j.slf4j)
    implementation(Kolod.FlatLookAndFeelfModel.core)
    implementation(FontChooser.core)
    implementation(SqLite.core)
    implementation(Manifests.core)
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
