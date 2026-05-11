package tools.konsole.core.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.core.Ansi
import tools.konsole.core.event.AnsiInputParser
import tools.konsole.core.event.Event
import tools.konsole.core.event.ModeState

/**
 * Tests for the Phase 6.5 parser extensions: OSC color responses (10/11/4),
 * DECRPM responses, and in-band window-resize notifications.
 *
 * Byte sequences here are the actual wire format real terminals emit (captured
 * from kitty, ghostty, and xterm where noted).
 */
class Phase65ParserSpec : StringSpec({

    fun feed(bytes: String): List<Event> {
        val parser = AnsiInputParser()
        val out = mutableListOf<Event>()
        for (ch in bytes) {
            val ev = parser.advance(ch.code)
            if (ev != null) out += ev
        }
        return out
    }

    // ---- OSC 11 (background color) ----

    "OSC 11 response with ST terminator → Event.BackgroundColor" {
        // ESC ] 11 ; rgb:0000/0000/0000 ESC \\  — pure black bg
        val seq = "${Ansi.OSC}11;rgb:0000/0000/0000${Ansi.ST}"
        val events = feed(seq)
        events shouldBe listOf(Event.BackgroundColor(0, 0, 0))
    }

    "OSC 11 response with BEL terminator → Event.BackgroundColor" {
        val seq = "${Ansi.OSC}11;rgb:ffff/ffff/ffff${Ansi.BEL}"
        val events = feed(seq)
        events shouldBe listOf(Event.BackgroundColor(255, 255, 255))
    }

    "OSC 11 with 2-digit hex components scales correctly" {
        val seq = "${Ansi.OSC}11;rgb:80/40/c0${Ansi.ST}"
        val events = feed(seq)
        events shouldBe listOf(Event.BackgroundColor(0x80, 0x40, 0xC0))
    }

    "OSC 11 with single-digit hex components scales correctly (*17)" {
        val seq = "${Ansi.OSC}11;rgb:8/4/c${Ansi.ST}"
        val events = feed(seq)
        events shouldBe listOf(Event.BackgroundColor(8 * 17, 4 * 17, 12 * 17))
    }

    // ---- OSC 10 (foreground color) ----

    "OSC 10 response → Event.ForegroundColor" {
        val seq = "${Ansi.OSC}10;rgb:dddd/dddd/dddd${Ansi.ST}"
        val events = feed(seq)
        events shouldBe listOf(Event.ForegroundColor(0xDD, 0xDD, 0xDD))
    }

    // ---- OSC 4 (palette color) ----

    "OSC 4 response → Event.PaletteColor with index" {
        val seq = "${Ansi.OSC}4;42;rgb:1234/5678/9abc${Ansi.ST}"
        val events = feed(seq)
        // 4-digit hex components: scale to 8-bit via >> 8
        events shouldBe listOf(Event.PaletteColor(42, 0x12, 0x56, 0x9A))
    }

    // ---- DECRPM ($y) ----

    "DECRPM CSI ?2026;1\$y → ModeReport(2026, Set)" {
        val seq = "${Ansi.CSI}?2026;1\$y"
        val events = feed(seq)
        events shouldBe listOf(Event.ModeReport(2026, ModeState.Set))
    }

    "DECRPM CSI ?9999;0\$y → ModeReport(9999, NotRecognized)" {
        val seq = "${Ansi.CSI}?9999;0\$y"
        val events = feed(seq)
        events shouldBe listOf(Event.ModeReport(9999, ModeState.NotRecognized))
    }

    "DECRPM CSI ?2004;3\$y → ModeReport(2004, PermanentlySet)" {
        val seq = "${Ansi.CSI}?2004;3\$y"
        val events = feed(seq)
        events shouldBe listOf(Event.ModeReport(2004, ModeState.PermanentlySet))
    }

    // ---- In-band window resize (CSI 48 ; rows ; cols ; ph ; pw t) ----

    "in-band resize CSI 48;rows;cols;ph;pw t → Event.Resize" {
        val seq = "${Ansi.CSI}48;24;80;480;640t"
        val events = feed(seq)
        events shouldBe listOf(Event.Resize(80, 24))
    }

    "in-band resize without pixel sizes still parses" {
        val seq = "${Ansi.CSI}48;30;120t"
        val events = feed(seq)
        events shouldBe listOf(Event.Resize(120, 30))
    }

    "other CSI ...t window-ops are silently consumed (no event)" {
        // CSI 8;rows;cols t is the request — replies start with 48 only.
        val seq = "${Ansi.CSI}3;100;200t"  // window-position report
        val events = feed(seq)
        events shouldBe emptyList()
    }

    // ---- Unrecognised OSC is silently consumed ----

    "unknown OSC tag is consumed without event" {
        val seq = "${Ansi.OSC}999;some payload${Ansi.ST}"
        val events = feed(seq)
        events shouldBe emptyList()
    }
})
