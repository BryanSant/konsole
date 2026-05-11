package tools.konsole.examples

import tools.konsole.rich.Console
import tools.konsole.rich.markdown.Markdown
import tools.konsole.rich.syntax.Syntax

/**
 * Phase 6 demo — Markdown rendering (including GFM tables) + syntax-highlighted
 * code via the konsole-rich Markdown and Syntax renderables.
 *
 *   ./gradlew :examples:runExample -Pexample=MarkdownAndSyntax
 */
public fun main() {
    val console = Console.system()

    val md = """
        |# konsole-rich Markdown demo
        |
        |This text is rendered via `Markdown(commonmark-java)` →
        |[konsole-rich Renderables](https://example.com/konsole).
        |
        |## A list
        |- **bold** inline
        |- *italic* inline
        |- `inline code`
        |
        |## A GFM table
        |
        || Name  | Score | Notes      |
        ||-------|-------|------------|
        || Alice |   99  | perfect    |
        || Bob   |   87  | very good  |
        || Carol |   65  | passable   |
        |
        |## A fenced code block
        |
        |```kotlin
        |fun main() {
        |    val console = Console.system()
        |    console.print("Hello, konsole")
        |}
        |```
        |
        |---
        |
        |> A blockquote that wraps onto multiple lines so you can see how the
        |> renderer handles long inline content within a `>`-quoted paragraph.
    """.trimMargin()

    console.print(Markdown(md))
    console.print()
    console.print("Standalone Syntax:")
    val source = """
        |class Foo(val name: String) {
        |    fun greet(): String = "Hello, ${'$'}name"
        |}
    """.trimMargin()
    console.print(Syntax(code = source, language = "kotlin", lineNumbers = true))
}
