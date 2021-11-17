@file:Suppress("MemberVisibilityCanBePrivate")

import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

fun PluginDependenciesSpec.shadow(module: String): PluginDependencySpec = id(module)
fun PluginDependenciesSpec.dokka(module: String): PluginDependencySpec = id(module)

object Shadow {
    const val version = "7.1.0"
    const val id = "com.github.johnrengelman.shadow"
}

object Jvm {
    const val version = "1.8"
}

object Kotlin {
    const val version = "1.5.31"
    const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
    const val dokka = "org.jetbrains.dokka:kotlin-as-java-plugin:$version"
    const val jvmId = "jvm"
    const val kaptId = "kapt"
    const val dokkaId = "org.jetbrains.dokka"
}

object Slf4j {
    const val version = "1.7.32"
    const val core = "org.slf4j:slf4j-log4j12:$version"
}

object Log4j {
    const val version = "2.14.1"
    const val core = "org.apache.logging.log4j:log4j-core:$version"
    const val api = "org.apache.logging.log4j:log4j-api:$version"
    const val slf4j = "org.apache.logging.log4j:log4j-slf4j-impl:$version"
}

object Manifests {
    const val version = "1.1"
    const val core = "com.jcabi:jcabi-manifests:$version"
}

object FontChooser {
    const val version = "2.4"
    const val core = "org.drjekyll:fontchooser:$version"
}

object SqLite {
    const val version = "3.36.0.3"
    const val core = "org.xerial:sqlite-jdbc:$version"
}

object Kolod {
    const val group = "io.github.kolod"
    object FlatLookAndFeelfModel {
        const val version = "1.1"
        const val core = "$group:flatlaf-themes-combobox-model:$version"
    }
}