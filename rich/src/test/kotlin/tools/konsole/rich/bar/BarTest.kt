package tools.konsole.rich.bar

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class BarTest : StringSpec({

    "Bar fills the active range" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Bar(size = 100.0, begin = 25.0, end = 75.0, width = 10))
        val out = sw.toString().trimEnd('\n')
        // 0..0.25 → 2 back, 0.25..0.75 → 5 fill, 0.75..1 → 3 back
        out.length shouldBe 10
    }

    "full range shows all complete chars" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Bar(size = 100.0, begin = 0.0, end = 100.0, width = 10))
        val out = sw.toString().trimEnd('\n')
        out shouldBe "━".repeat(10)
    }

    "empty range shows all back chars" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 10, colorSystem = ColorSystem.None)
        c.print(Bar(size = 100.0, begin = 0.0, end = 0.0, width = 10))
        val out = sw.toString().trimEnd('\n')
        out shouldBe "─".repeat(10)
    }
})
