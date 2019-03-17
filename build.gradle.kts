import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.cloud.tools.gradle.appengine.core.DeployExtension
import com.google.cloud.tools.gradle.appengine.standard.RunExtension

group = "by.mksn.gae"
version = "0.1"

val kotlinVersion = "1.3.21"
val ktorVersion = "1.1.3"

plugins {
    java
    war
    kotlin("jvm") version "1.3.21"
    id("com.google.cloud.tools.appengine") version "1.3.4"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
    maven { url = uri("https://jitpack.io") }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

fun ktor(m: String = "", v: String) = "io.ktor:ktor${if (m.startsWith('-') || m.isBlank()) m else "-$m"}:$v"

dependencies {

    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile(ktor(v = ktorVersion))
    compile(ktor("gson", ktorVersion))
    compile(ktor("html-builder", ktorVersion))
    compile(ktor("client-core", ktorVersion))
    compile(ktor("client-core-jvm", ktorVersion))
    compile(ktor("client-json", ktorVersion))
    compile(ktor("client-json-jvm", ktorVersion))
    compile(ktor("client-gson", ktorVersion))
    compile(ktor("client-android", ktorVersion))
    compile(ktor("server-servlet", ktorVersion))
    compile("io.github.seik.kotlin-telegram-bot:telegram:0.3.5")
    compile("com.google.appengine:appengine-api-1.0-sdk:1.9.71")
    compile("commons-io:commons-io:2.6")
    compile("com.google.cloud:google-cloud-logging-logback:0.67.0-alpha")
    compile("com.google.http-client:google-http-client:1.23.0")

    testCompile("junit", "junit", "4.12")
}

appengine {
    ((this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("run") as RunExtension).apply {
        port = 8888
        jvmFlags = listOf("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8889")
    }
    ((this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("deploy") as DeployExtension).apply {
        project = "telegram-currency-bot"
        stopPreviousVersion = true
        version = "1"
    }

}
