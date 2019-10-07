import com.google.cloud.tools.gradle.appengine.core.DeployExtension
import com.google.cloud.tools.gradle.appengine.standard.RunExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "by.mksn.gae"
version = "0.1"

val kotlinVersion = "1.3.50"
val ktorVersion = "1.2.4"

plugins {
    java
    war
    kotlin("jvm") version "1.3.50"
    id("com.google.cloud.tools.appengine") version "1.3.4"
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://dl.bintray.com/hotkeytlt/maven") }
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
    compile(ktor("client-apache", ktorVersion))
    compile(ktor("server-servlet", ktorVersion))

    compile("io.github.seik.kotlin-telegram-bot:telegram:0.3.5")
    compile("com.google.appengine:appengine-api-1.0-sdk:1.9.76")
    compile("commons-io:commons-io:2.6")
    compile("org.slf4j:slf4j-nop:1.7.28")
    compile("com.google.http-client:google-http-client:1.32.1")
    compile("io.github.config4k:config4k:0.4.1")
    compile("com.github.h0tk3y.betterParse:better-parse-jvm:0.4.0-alpha-3")
    compile("com.atlassian.commonmark:commonmark:0.12.1")



    testCompile("junit", "junit", "4.12")
}

appengine {
    ((this as ExtensionAware).extensions.getByName("run") as RunExtension).apply {
        port = 8888
        jvmFlags = listOf("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8889")
    }
    ((this as ExtensionAware).extensions.getByName("deploy") as DeployExtension).apply {
        project = "telegram-currency-bot"
        stopPreviousVersion = true
        version = "1"
    }

}
