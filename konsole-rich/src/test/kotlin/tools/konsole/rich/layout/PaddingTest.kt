package tools.konsole.rich.layout

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Text
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class PaddingTest : StringSpec({

    "1-value shorthand pads all sides" {
        val p = Padding.of(2)
        p shouldBe Padding(2, 2, 2, 2)
    }

    "2-value shorthand sets vertical and horizontal" {
        val p = Padding.of(1, 3)
        p shouldBe Padding(1, 3, 1, 3)
    }

    "4-value shorthand sets each side independently" {
        val p = Padding.of(1, 2, 3, 4)
        p shouldBe Padding(1, 2, 3, 4)
    }

    "Padded renders with horizontal pad" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Padded(Text("hi"), Padding(0, 1, 0, 1)))
        // expand=false (default): no fill to console width — just left + content + right pad
        sw.toString() shouldBe " hi \n"
    }

    "Padded with expand=true fills console width" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Padded(Text("hi"), Padding(0, 1, 0, 1), expand = true))
        // 1 left pad + "hi" + (8 inner - 2 cells = 6) fill + 1 right pad = 10
        sw.toString() shouldBe " hi       \n"
    }

    "Padded renders with top and bottom pads" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Padded(Text("hi"), Padding(1, 0, 1, 0)))
        // 1 line of nothing/blank, "hi", 1 line of blank
        val out = sw.toString()
        out.lines().size shouldBe 4 // 3 visible lines + trailing newline becomes 4 entries
    }
})
