package tools.konsole.rich.markdown

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class MarkdownTest : StringSpec({

    "headings render with prefix and rule" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Markdown("# Hello\n\nbody"))
        val out = sw.toString()
        out shouldContain "Hello"
        out shouldContain "body"
        out shouldContain "#"
    }

    "bullet list renders bullets" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Markdown("- one\n- two\n- three"))
        val out = sw.toString()
        out shouldContain "•"
        out shouldContain "one"
        out shouldContain "two"
        out shouldContain "three"
    }

    "ordered list renders numbers" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Markdown("1. apple\n2. banana"))
        val out = sw.toString()
        out shouldContain "1."
        out shouldContain "2."
    }

    "fenced code block invokes Syntax" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Markdown("""before

```kotlin
val x = 1
```

after"""))
        val out = sw.toString()
        out shouldContain "val"
        out shouldContain "x"
    }

    "blockquote renders with bar prefix" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Markdown("> quoted text"))
        val out = sw.toString()
        out shouldContain "│"
        out shouldContain "quoted text"
    }
})
