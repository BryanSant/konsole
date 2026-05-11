@file:Suppress("UnstableApiUsage")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
    id("com.gradleup.nmcp.settings") version "1.4.4"
}

rootProject.name = "konsole-parent"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

nmcpSettings {
    centralPortal {
        username = System.getenv("MVN_CENTRAL_USER") ?: ""
        password = System.getenv("MVN_CENTRAL_PASS") ?: ""
        publishingType = "AUTOMATIC"
    }
}

include(":konsole-core", ":konsole-rich", ":konsole-textual", ":examples")
