package tools.konsole.rich.json

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class JsonTest : StringSpec({

    "fromData renders nested map" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Json.fromData(mapOf("name" to "alice", "age" to 30, "tags" to listOf("a", "b"))))
        val out = sw.toString()
        out shouldContain "name"
        out shouldContain "alice"
        out shouldContain "age"
        out shouldContain "30"
        out shouldContain "tags"
    }

    "raw JSON string is reformatted" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Json("""{"x":1,"y":[2,3]}"""))
        val out = sw.toString()
        // Must have indentation
        out shouldContain "  "
        out shouldContain "x"
        out shouldContain "1"
    }

    "fromData null literal" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Json.fromData(mapOf("x" to null)))
        val out = sw.toString()
        out shouldContain "null"
    }
})
