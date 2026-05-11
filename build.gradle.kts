plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

group = "io.github.bryansant.konsole"
version = "0.1.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}
