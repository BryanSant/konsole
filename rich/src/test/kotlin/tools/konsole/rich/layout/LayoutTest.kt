package tools.konsole.rich.layout

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Text
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class LayoutTest : StringSpec({

    "leaf layout renders content padded to size" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, height = 3, colorSystem = ColorSystem.None)
        val layout = Layout(name = "root", renderable = Text("hi"))
        c.print(layout)
        val lines = sw.toString().lines()
        // 3 lines: hi padded to width 10, then 2 empty lines
        lines.size shouldBe 4 // 3 visible + trailing newline
        lines[0].length shouldBe 10
    }

    "split row creates side-by-side regions" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, height = 1, colorSystem = ColorSystem.None)
        val layout = Layout(name = "root").splitRow(
            Layout(name = "l", renderable = Text("L")),
            Layout(name = "r", renderable = Text("R")),
        )
        c.print(layout)
        val out = sw.toString()
        out shouldContain "L"
        out shouldContain "R"
    }

    "ratio and size budgeting" {
        val children = listOf(
            Layout(name = "a", size = 3),
            Layout(name = "b", ratio = 1),
            Layout(name = "c", ratio = 2),
        )
        val parent = Layout().splitRow(*children.toTypedArray())
        // We can't directly call budget(); the test exercises rendering width.
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 12, height = 1, colorSystem = ColorSystem.None)
        c.print(parent)
        val out = sw.toString().trimEnd('\n')
        out.length shouldBe 12
    }

    "lookup by name" {
        val root = Layout(name = "root").splitColumn(
            Layout(name = "header"),
            Layout(name = "body"),
        )
        (root["header"] != null) shouldBe true
        (root["body"] != null) shouldBe true
        (root["nonexistent"] == null) shouldBe true
    }

    "update swaps content" {
        val l = Layout(name = "x", renderable = Text("first"))
        l.update(Text("second"))
        l.renderable.toString() shouldContain "second"
    }
})
