import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    signing
}

base {
    archivesName.set("konsole-textual")
}

kotlin {
    jvmToolchain(25)
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
        )
    }
}

dependencies {
    api(project(":rich"))
    implementation(libs.kotlin.reflect)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.assertions.core)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets.all {
                testTask.configure {
                    useJUnitPlatform()
                    jvmArgs("--enable-native-access=ALL-UNNAMED")
                    systemProperty("kotest.tags.exclude", System.getProperty("kotest.tags.exclude", "Tty"))
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "konsole-textual"
            pom {
                name.set("konsole-textual")
                description.set("Konsole textual port — App, Screen, Widget, Driver, TCSS, built-in widgets.")
                url.set("https://github.com/bryansant/konsole")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("bryansant")
                        name.set("Bryan Sant")
                    }
                }
                scm {
                    url.set("https://github.com/bryansant/konsole")
                    connection.set("scm:git:git://github.com/bryansant/konsole.git")
                    developerConnection.set("scm:git:ssh://git@github.com/bryansant/konsole.git")
                }
            }
        }
    }
}

signing {
    val key = System.getenv("GPG_PRIVATE_KEY")
    val pwd = System.getenv("GPG_PASSPHRASE")
    if (!key.isNullOrBlank()) {
        useInMemoryPgpKeys(key, pwd)
        sign(publishing.publications["maven"])
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
