import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    signing
}

base {
    archivesName.set("konsole-rich")
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
    api(project(":core"))
    implementation(libs.commonmark)
    implementation(libs.commonmark.gfm.tables)

    // Tree-sitter — used by TreeSitterLexer for syntax highlighting in
    // TextArea / Syntax. compileOnly + runtimeOnly so the native libs are
    // available when used but consumers who don't touch syntax highlighting
    // don't pay the ~30MB native-library cost.
    compileOnly(libs.treesitter)
    runtimeOnly(libs.treesitter)
    runtimeOnly(libs.treesitter.kotlin)
    runtimeOnly(libs.treesitter.java)
    runtimeOnly(libs.treesitter.python)
    runtimeOnly(libs.treesitter.json)
    runtimeOnly(libs.treesitter.bash)
    runtimeOnly(libs.treesitter.markdown)
    runtimeOnly(libs.treesitter.javascript)
    runtimeOnly(libs.treesitter.typescript)
    runtimeOnly(libs.treesitter.rust)
    runtimeOnly(libs.treesitter.go)
    runtimeOnly(libs.treesitter.yaml)
    runtimeOnly(libs.treesitter.toml)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.treesitter)
    testImplementation(libs.treesitter.kotlin)
    testImplementation(libs.treesitter.java)
    testImplementation(libs.treesitter.python)
    testImplementation(libs.treesitter.json)
    testImplementation(libs.treesitter.bash)
    testImplementation(libs.treesitter.markdown)
    testImplementation(libs.treesitter.javascript)
    testImplementation(libs.treesitter.typescript)
    testImplementation(libs.treesitter.rust)
    testImplementation(libs.treesitter.go)
    testImplementation(libs.treesitter.yaml)
    testImplementation(libs.treesitter.toml)
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
            artifactId = "konsole-rich"
            pom {
                name.set("konsole-rich")
                description.set("Konsole rich port — Console, Segment, Style, renderables (Panel, Table, Tree, Layout, Live, Progress, Markdown, Syntax).")
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
