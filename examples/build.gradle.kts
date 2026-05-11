import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(25)
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
    implementation(project(":core"))
    implementation(project(":rich"))
    implementation(project(":textualize"))
}

tasks.register<JavaExec>("runExample") {
    group = "application"
    description = "Run an example: ./gradlew :examples:runExample -Pexample=HelloKonsole. " +
        "WARNING: Gradle's JavaExec cannot pass a controlling TTY to the child JVM, so " +
        "demos that need a real terminal (systemDriver) will see size 0x0. Use ./demo.sh instead."
    classpath = sourceSets.main.get().runtimeClasspath
    val name = providers.gradleProperty("example").orElse("HelloKonsole")
    mainClass.set(name.map { n ->
        val dotted = n.replace('/', '.')
        "tools.konsole.examples.${dotted}Kt"
    })
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Dorg.jline.terminal.provider=ffm",
    )
    standardInput = System.`in`
}

// Print the runtime classpath so a shell launcher can `exec java` directly,
// preserving the calling terminal's TTY (Gradle's JavaExec can't do this).
tasks.register("printRuntimeClasspath") {
    group = "application"
    description = "Print the runtime classpath for the examples module."
    val cp = sourceSets.main.get().runtimeClasspath
    doLast { println(cp.asPath) }
}

// Print the toolchain-resolved java executable so a shell launcher uses the
// same JDK Gradle compiles with (Java 25+ for FFM). Whatever `java` is on the
// user's PATH may be older; FFM is final since Java 22.
tasks.register("printJavaLauncher") {
    group = "application"
    description = "Print the absolute path to the toolchain's java binary."
    val launcher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    doLast { println(launcher.get().executablePath.asFile.absolutePath) }
}
