package tools.konsole.rich

import tools.konsole.core.ColorSystem

import tools.konsole.core.Ansi
import tools.konsole.rich.text.Justify
import tools.konsole.core.style.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.StringWriter

class PipelineTest : StringSpec({

    "Console.string captures plain text" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.TrueColor)
        c.print("hello world")
        sw.toString() shouldBe "hello world\n"
    }

    "markup produces SGR escapes" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.TrueColor)
        c.print("[bold red]hi[/]")
        val out = sw.toString()
        out shouldContain "hi"
        // SGR opener and reset should both appear
        out shouldContain "${Ansi.CSI}"
        out shouldContain "${Ansi.CSI}0m"
    }

    "ColorSystem.None strips colors" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print("[bold red]hi[/]")
        val out = sw.toString()
        out shouldContain "hi"
        // Reset code can still occur, but no fg color codes (38;2; / 38;5;)
        out shouldNotContain "38;2;"
        out shouldNotContain "38;5;"
    }

    "right justify pads on the left" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Text("hi"), PrintOptions(justify = Justify.Right))
        sw.toString() shouldBe "        hi\n"
    }

    "center justify pads both sides" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Text("hi"), PrintOptions(justify = Justify.Center))
        // 8 padding, split 4/4
        sw.toString() shouldBe "    hi    \n"
    }

    "rule renders horizontal divider" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.rule(style = Style.NULL)
        sw.toString() shouldBe "──────────\n"
    }

    "rule with title centers it" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 12, colorSystem = ColorSystem.None)
        c.rule(title = "x", style = Style.NULL)
        sw.toString() shouldBe "──── x ─────\n"
    }

    "wrap breaks long lines on word boundaries" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print("hello there friend")
        val lines = sw.toString().trimEnd('\n').split("\n")
        lines.forEach { it.length shouldBe (10).coerceAtMost(it.length) }
        lines.joinToString(" ") { it.trim() } shouldBe "hello there friend"
    }

    "no_color from constructor strips" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Text("hi", style = Style(color = Color.Red)))
        val out = sw.toString()
        out shouldNotContain "38;2;"
        out shouldNotContain "38;5;"
    }

    "useTheme swaps then restores" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.TrueColor)
        val original = c.theme
        c.useTheme(Theme(mapOf("x" to Style(color = Color.Cyan)))) {
            c.theme.styles.containsKey("x") shouldBe true
        }
        (c.theme === original) shouldBe true
    }

    "log emits a level prefix" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.log("hello")
        val out = sw.toString()
        out shouldContain "INFO"
        out shouldContain "hello"
    }

    "capture redirects to in-memory writer" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val cr = c.capture { inner -> inner.print("captured") }
        cr.output shouldBe "captured\n"
        sw.toString() shouldBe ""
    }
})
