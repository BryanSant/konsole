package tools.konsole.rich.traceback

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class TracebackTest : StringSpec({

    "Throwable renders with class name and message" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        try {
            error("kaboom")
        } catch (t: Throwable) {
            c.print(Traceback(t))
        }
        val out = sw.toString()
        out shouldContain "IllegalStateException"
        out shouldContain "kaboom"
        out shouldContain "Traceback"
    }

    "cause chain renders both exceptions" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val cause = IllegalArgumentException("inner")
        val outer = RuntimeException("outer", cause)
        c.print(Traceback(outer))
        val out = sw.toString()
        out shouldContain "outer"
        out shouldContain "inner"
        out shouldContain "direct cause"
    }

    "Console.print(throwable) routes through Traceback" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(IllegalStateException("oh no"))
        val out = sw.toString()
        out shouldContain "IllegalStateException"
        out shouldContain "oh no"
    }
})
