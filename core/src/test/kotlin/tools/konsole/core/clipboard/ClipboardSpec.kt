package tools.konsole.core.clipboard

import tools.konsole.core.ClipboardDestination
import tools.konsole.core.ClipboardTooLargeException
import tools.konsole.core.Command
import tools.konsole.core.CopyToClipboard
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

private fun ansi(c: Command): String = StringBuilder().also(c::writeAnsi).toString()

class ClipboardSpec : StringSpec({

    val esc = "\u001B"
    val osc = "$esc]"
    val st = "$esc\\"

    "CopyToClipboard emits OSC 52;c;<base64>ST for the default clipboard" {
        val s = ansi(CopyToClipboard("hello"))
        s shouldStartWith "${osc}52;c;"
        s shouldContain "aGVsbG8="
        s shouldEndWith st
    }

    "Multiple destinations concatenate Pc letters" {
        val s = ansi(CopyToClipboard("x", destinations = setOf(ClipboardDestination.Clipboard, ClipboardDestination.Primary)))
        val between = s.substringAfter("52;").substringBefore(";")
        between.toSet() shouldBe setOf('c', 'p')
    }

    "Other(letter) is honored" {
        val s = ansi(CopyToClipboard("x", destinations = setOf(ClipboardDestination.Other('s'))))
        s shouldContain "52;s;"
    }

    "Empty destinations is rejected at construction" {
        shouldThrow<IllegalArgumentException> {
            CopyToClipboard("x", destinations = emptySet())
        }
    }

    "maxBytes is enforced" {
        shouldThrow<ClipboardTooLargeException> {
            ansi(CopyToClipboard("xxxxx", maxBytes = 4))
        }
    }
})
