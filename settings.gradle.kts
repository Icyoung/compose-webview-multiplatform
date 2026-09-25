rootProject.name = "compose-webview-build"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/kpm/public/")
        maven("https://jitpack.io")
        maven("https://jogamp.org/deployment/maven")
    }
}

include(":webview")
project(":webview").name = "compose-webview-multiplatform"
if (providers.gradleProperty("webview.samples").orNull == "true") {
    include(":sample:androidApp")
    include(":sample:desktopApp")
    include(":sample:wasmJsApp")
    include(":sample:shared")
}
