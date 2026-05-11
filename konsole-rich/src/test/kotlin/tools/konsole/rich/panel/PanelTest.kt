package tools.konsole.rich.panel

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class PanelTest : StringSpec({

    "Panel renders ROUNDED border with content" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Panel(Text("hi"), box = Box.ROUNDED))
        // Expected:
        // ╭────────╮
        // │   hi   │  (1 left + content + filler + 1 right)
        // ╰────────╯
        val out = sw.toString()
        out shouldContain "╭"
        out shouldContain "╮"
        out shouldContain "╰"
        out shouldContain "╯"
        out shouldContain "│"
        out shouldContain "hi"
    }

    "Panel with title shows title in top border" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 20, colorSystem = ColorSystem.None)
        c.print(Panel(Text("body"), title = Text("hi"), box = Box.SQUARE))
        val out = sw.toString()
        out shouldContain "hi"
        out shouldContain "┌"
        out shouldContain "┐"
        out shouldContain "└"
        out shouldContain "┘"
    }

    "Panel with subtitle shows subtitle in bottom border" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 20, colorSystem = ColorSystem.None)
        c.print(Panel(Text("body"), subtitle = Text("foot"), box = Box.SQUARE))
        val out = sw.toString()
        out shouldContain "foot"
    }

    "Panel.of helper accepts strings" {
        val p = Panel.of("body", "title")
        p.title shouldNotBe null
    }
})

private infix fun <T> T.shouldNotBe(expected: T?) {
    if (this == expected) error("expected not equal to $expected but was $this")
}
