package tools.konsole.rich.layout

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Text
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class GroupTest : StringSpec({

    "Group renders children in sequence" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Group(Text("a"), Text("b"), Text("c")))
        sw.toString() shouldBe "a\nb\nc\n"
    }

    "group convenience function" {
        val g = group(Text("x"), Text("y"))
        g.children.size shouldBe 2
    }
})
