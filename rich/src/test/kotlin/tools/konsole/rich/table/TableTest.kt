package tools.konsole.rich.table

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.box.Box
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class TableTest : StringSpec({

    "minimal table" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 40, colorSystem = ColorSystem.None)
        val t = Table(box = Box.SQUARE).apply {
            addColumn("Name")
            addColumn("Score")
            addRow("alice", "100")
            addRow("bob", "92")
        }
        c.print(t)
        val out = sw.toString()
        out shouldContain "Name"
        out shouldContain "Score"
        out shouldContain "alice"
        out shouldContain "100"
        out shouldContain "bob"
        out shouldContain "92"
        out shouldContain "┌"
        out shouldContain "└"
    }

    "title and caption" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 40, colorSystem = ColorSystem.None)
        val t = Table(
            title = tools.konsole.rich.Text("My Title"),
            caption = tools.konsole.rich.Text("My Caption"),
            box = Box.SQUARE,
        ).apply {
            addColumn("k")
            addRow("v")
        }
        c.print(t)
        val out = sw.toString()
        out shouldContain "My Title"
        out shouldContain "My Caption"
    }

    "show_lines puts dividers between rows" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 30, colorSystem = ColorSystem.None)
        val t = Table(box = Box.SQUARE, showLines = true).apply {
            addColumn("a")
            addRow("1")
            addRow("2")
        }
        c.print(t)
        val out = sw.toString()
        // Row divider char from SQUARE box is ├─┼┤
        out shouldContain "├"
    }

    "no box uses no borders" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 30, colorSystem = ColorSystem.None)
        val t = Table(box = null, showHeader = false).apply {
            addColumn("a")
            addColumn("b")
            addRow("1", "2")
        }
        c.print(t)
        val out = sw.toString()
        out shouldContain "1"
        out shouldContain "2"
    }
})
