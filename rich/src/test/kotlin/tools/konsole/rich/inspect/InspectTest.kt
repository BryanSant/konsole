package tools.konsole.rich.inspect

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

private data class Sample(val name: String, val age: Int, val tags: List<String>)

class InspectTest : StringSpec({

    "Inspect shows class and field values" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 100, colorSystem = ColorSystem.None)
        c.inspect(Sample("alice", 30, listOf("a", "b")))
        val out = sw.toString()
        out shouldContain "Sample"
        out shouldContain "name"
        out shouldContain "alice"
        out shouldContain "age"
        out shouldContain "30"
    }

    "Inspect on Throwable shows message and frames" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 100, colorSystem = ColorSystem.None)
        val ex = try { error("boom") } catch (t: Throwable) { t }
        c.inspect(ex)
        val out = sw.toString()
        out shouldContain "IllegalStateException"
        out shouldContain "boom"
        out shouldContain "stack trace"
    }

    "Inspect on null shows None" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.inspect(null)
        val out = sw.toString()
        out shouldContain "None"
    }
})
