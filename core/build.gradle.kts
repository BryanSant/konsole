import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    signing
}

// Keep the published artifact / jar name as "konsole-core" even though the
// Gradle project directory is just "core" (the parent dir is already "konsole").
base {
    archivesName.set("konsole-core")
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
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.io.core)
    api(libs.jline.terminal)
    runtimeOnly(libs.jline.terminal.ffm)

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

tasks.register<Test>("ttyTest") {
    description = "Run TTY-required integration tests."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("kotest.tags.include", "Tty")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "konsole-core"
            pom {
                name.set("konsole-core")
                description.set("Konsole core terminal primitives — JLine FFM wrapper, ANSI commands, input parser.")
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
