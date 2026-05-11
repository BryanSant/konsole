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
    description = "Run an example: ./gradlew :examples:runExample -Pexample=HelloKonsole"
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
