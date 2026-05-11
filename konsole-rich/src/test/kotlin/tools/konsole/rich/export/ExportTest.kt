package tools.konsole.rich.export

import tools.konsole.core.Ansi
import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class ExportTest : StringSpec({

    "exportText returns plain text from record buffer" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.TrueColor, record = true)
        c.print("[bold red]hello[/]")
        c.print("plain")
        val text = c.exportText()
        text shouldContain "hello"
        text shouldContain "plain"
        // No SGR escapes in exported text
        text.contains(Ansi.CSI) shouldBe false
    }

    "exportHtml wraps styled segments in spans" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.TrueColor, record = true)
        c.print(Text("hello", style = Style(color = tools.konsole.core.style.Color.Red, bold = true)))
        val html = c.exportHtml()
        html shouldContain "<pre"
        html shouldContain "<span"
        html shouldContain "color:#ff0000"
        html shouldContain "font-weight:bold"
        html shouldContain "hello"
    }

    "exportSvg produces an svg document" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 40, colorSystem = ColorSystem.TrueColor, record = true)
        c.print("hi")
        val svg = c.exportSvg()
        svg shouldContain "<svg"
        svg shouldContain "</svg>"
        svg shouldContain "hi"
    }
})
