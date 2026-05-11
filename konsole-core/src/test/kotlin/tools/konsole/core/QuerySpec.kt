package tools.konsole.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun ansi(c: Command): String = StringBuilder().also(c::writeAnsi).toString()

class QuerySpec : StringSpec({

    "QueryBackgroundColor emits OSC 11;? ST" {
        ansi(QueryBackgroundColor) shouldBe "${Ansi.OSC}11;?${Ansi.ST}"
    }

    "QueryForegroundColor emits OSC 10;? ST" {
        ansi(QueryForegroundColor) shouldBe "${Ansi.OSC}10;?${Ansi.ST}"
    }

    "QueryPaletteColor emits OSC 4;<idx>;? ST" {
        ansi(QueryPaletteColor(0)) shouldBe "${Ansi.OSC}4;0;?${Ansi.ST}"
        ansi(QueryPaletteColor(255)) shouldBe "${Ansi.OSC}4;255;?${Ansi.ST}"
        ansi(QueryPaletteColor(42)) shouldBe "${Ansi.OSC}4;42;?${Ansi.ST}"
    }

    "QueryPaletteColor rejects out-of-range index" {
        try {
            QueryPaletteColor(-1)
            error("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { /* ok */ }
        try {
            QueryPaletteColor(256)
            error("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { /* ok */ }
    }
})
