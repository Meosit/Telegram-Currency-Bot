rootProject.name = "EasyCurrBot"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        maven { url = uri("https://repo1.maven.org/maven2/") }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "appengine") {
                useModule("com.google.appengine:gradle-appengine-plugin:${requested.version}")
            }
            if (requested.id.id == "com.google.cloud.tools.appengine") {
                useModule("com.google.cloud.tools:appengine-gradle-plugin:${requested.version}")
            }
        }
    }
}