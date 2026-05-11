package tools.konsole.rich.layout

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Text
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class AlignTest : StringSpec({

    "Align.center centers within console width" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Align.center(Text("hi")))
        sw.toString() shouldBe "    hi    \n"
    }

    "Align.right pads on the left" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Align.right(Text("hi")))
        sw.toString() shouldBe "        hi\n"
    }

    "Align.left does not pad" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Align.left(Text("hi"), pad = false))
        sw.toString() shouldBe "hi\n"
    }

    "Align with explicit width truncates context" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 30, colorSystem = ColorSystem.None)
        c.print(Align.center(Text("hi"), width = 10))
        sw.toString() shouldBe "    hi    \n"
    }
})
