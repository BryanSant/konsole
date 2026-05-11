package tools.konsole.core.parser

import tools.konsole.core.event.AnsiInputParser
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private const val ESC = "\u001B"

private fun feed(parser: AnsiInputParser, bytes: String) {
    for (b in bytes.toByteArray(Charsets.US_ASCII)) parser.advance(b.toInt() and 0xFF)
}

class ParserCallbackSpec : StringSpec({

    "cursor-position response routes to the onCursorPosition callback (0-indexed)" {
        val parser = AnsiInputParser()
        var pos: Pair<Int, Int>? = null
        parser.onCursorPosition = { c, r -> pos = c to r }

        // CSI 5;10R → row=5, col=10 (1-indexed). Expect 0-indexed (col=9, row=4).
        feed(parser, "$ESC[5;10R")
        pos shouldBe (9 to 4)
    }

    "kitty keyboard-flags response routes to onKeyboardFlags" {
        val parser = AnsiInputParser()
        var bits: Int? = null
        parser.onKeyboardFlags = { bits = it }

        feed(parser, "$ESC[?7u")
        bits shouldBe 7
    }

    "callbacks don't break subsequent event parsing" {
        val parser = AnsiInputParser()
        var pos: Pair<Int, Int>? = null
        parser.onCursorPosition = { c, r -> pos = c to r }

        feed(parser, "$ESC[3;4R")
        val ev = parser.advance('A'.code)
        pos shouldBe (3 to 2)
        // 'A' should produce a Char event
        ev?.toString()?.contains("c=A") shouldBe true
    }
})
