package tools.konsole.rich.pretty

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class PrettyTest : StringSpec({

    "primitives" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Pretty(42))
        sw.toString() shouldContain "42"
    }

    "string is quoted" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Pretty("hello"))
        sw.toString() shouldContain "\"hello\""
    }

    "list and map" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Pretty(listOf(1, 2, 3)))
        sw.toString() shouldContain "[1, 2, 3]"

        val sw2 = StringWriter()
        val c2 = Console(terminal = null, writer = sw2, width = 80, colorSystem = ColorSystem.None)
        c2.print(Pretty(mapOf("a" to 1, "b" to 2)))
        val out = sw2.toString()
        out shouldContain "{"
        out shouldContain "\"a\": 1"
        out shouldContain "\"b\": 2"
        out shouldContain "}"
    }

    "null prints as null" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Pretty(null))
        sw.toString() shouldContain "null"
    }

    "data class shows class and fields" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        data class Point(val x: Int, val y: Int)
        c.print(Pretty(Point(3, 4)))
        val out = sw.toString()
        out shouldContain "Point"
        out shouldContain "x="
        out shouldContain "3"
        out shouldContain "y="
        out shouldContain "4"
    }
})
