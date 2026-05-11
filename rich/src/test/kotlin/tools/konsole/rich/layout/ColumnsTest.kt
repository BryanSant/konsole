package tools.konsole.rich.layout

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Text
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class ColumnsTest : StringSpec({

    "Columns lays out items horizontally" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Columns(listOf(Text("aa"), Text("bb"), Text("cc"))))
        val out = sw.toString()
        out shouldContain "aa"
        out shouldContain "bb"
        out shouldContain "cc"
        // Should fit on one line within 80 cols
        out.trimEnd('\n').lines().size.let { check(it == 1) { "expected 1 line, got $it" } }
    }
})
