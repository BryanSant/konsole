package tools.konsole.core.parser

import tools.konsole.core.event.Event
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyEvent
import tools.konsole.core.event.KeyEventKind
import tools.konsole.core.event.KeyEventState
import tools.konsole.core.event.KeyModifiers
import tools.konsole.core.event.MediaKeyCode
import tools.konsole.core.event.ModifierKeyCode
import tools.konsole.core.event.MouseButton
import tools.konsole.core.event.MouseEvent
import tools.konsole.core.event.MouseEventKind
import tools.konsole.core.event.AnsiInputParser
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun feed(parser: AnsiInputParser, bytes: ByteArray): List<Event> {
    val out = mutableListOf<Event>()
    for (b in bytes) parser.advance(b.toInt() and 0xFF)?.let { out += it }
    return out
}

private fun feed(parser: AnsiInputParser, s: String): List<Event> =
    feed(parser, s.toByteArray(Charsets.UTF_8))

private fun parseAll(bytes: ByteArray): List<Event> {
    val p = AnsiInputParser()
    val out = feed(p, bytes).toMutableList()
    p.flush()?.let { out += it }
    return out
}

private fun parseAll(s: String): List<Event> = parseAll(s.toByteArray(Charsets.UTF_8))

/** Parse a sequence prefixed with ESC (0x1B). */
private fun parseEsc(rest: String): List<Event> {
    val tail = rest.toByteArray(Charsets.UTF_8)
    val all = ByteArray(1 + tail.size)
    all[0] = 0x1B
    System.arraycopy(tail, 0, all, 1, tail.size)
    return parseAll(all)
}

private fun key(code: KeyCode, mods: KeyModifiers = KeyModifiers.NONE): Event =
    Event.Key(KeyEvent(code, mods))

private fun keyEv(code: KeyCode, mods: KeyModifiers, kind: KeyEventKind, state: KeyEventState = KeyEventState.NONE): Event =
    Event.Key(KeyEvent(code, mods, kind, state))

class AnsiInputParserSpec : StringSpec({

    // --- single bytes ----------------------------------------------------

    "lowercase ASCII letter" {
        parseAll("a") shouldContainExactly listOf(key(KeyCode.Char('a')))
    }

    "uppercase ASCII letter has SHIFT modifier" {
        parseAll("C") shouldContainExactly listOf(key(KeyCode.Char('C'), KeyModifiers.SHIFT))
    }

    "digit char" {
        parseAll("7") shouldContainExactly listOf(key(KeyCode.Char('7')))
    }

    "punctuation char" {
        parseAll("!") shouldContainExactly listOf(key(KeyCode.Char('!')))
    }

    "Tab byte" {
        parseAll(byteArrayOf(0x09)) shouldContainExactly listOf(key(KeyCode.Tab))
    }

    "CR byte yields Enter" {
        parseAll(byteArrayOf(0x0D)) shouldContainExactly listOf(key(KeyCode.Enter))
    }

    "Backspace 0x7F" {
        parseAll(byteArrayOf(0x7F)) shouldContainExactly listOf(key(KeyCode.Backspace))
    }

    "Backspace 0x08" {
        parseAll(byteArrayOf(0x08)) shouldContainExactly listOf(key(KeyCode.Backspace))
    }

    "Ctrl+a (0x01)" {
        parseAll(byteArrayOf(0x01)) shouldContainExactly
            listOf(key(KeyCode.Char('a'), KeyModifiers.CONTROL))
    }

    "Ctrl+t (0x14)" {
        parseAll(byteArrayOf(0x14)) shouldContainExactly
            listOf(key(KeyCode.Char('t'), KeyModifiers.CONTROL))
    }

    "Ctrl+space (0x00)" {
        parseAll(byteArrayOf(0x00)) shouldContainExactly
            listOf(key(KeyCode.Char(' '), KeyModifiers.CONTROL))
    }

    "Ctrl+4 (0x1C)" {
        parseAll(byteArrayOf(0x1C)) shouldContainExactly
            listOf(key(KeyCode.Char('4'), KeyModifiers.CONTROL))
    }

    // --- Esc ------------------------------------------------------------

    "lone Esc resolves on flush" {
        val p = AnsiInputParser()
        p.advance(0x1B) shouldBe null
        p.flush() shouldBe key(KeyCode.Esc)
    }

    "Esc Esc yields one Esc immediately" {
        val p = AnsiInputParser()
        p.advance(0x1B) shouldBe null
        p.advance(0x1B) shouldBe key(KeyCode.Esc)
    }

    "Alt+letter (Esc + char)" {
        parseAll(byteArrayOf(0x1B, 'c'.code.toByte())) shouldContainExactly
            listOf(key(KeyCode.Char('c'), KeyModifiers.ALT))
    }

    "Alt+Shift+letter via Esc-prefix" {
        parseAll(byteArrayOf(0x1B, 'H'.code.toByte())) shouldContainExactly
            listOf(key(KeyCode.Char('H'), KeyModifiers.ALT + KeyModifiers.SHIFT))
    }

    "Alt+Ctrl+letter via Esc-prefix" {
        parseAll(byteArrayOf(0x1B, 0x14)) shouldContainExactly
            listOf(key(KeyCode.Char('t'), KeyModifiers.ALT + KeyModifiers.CONTROL))
    }

    // --- SS3 -------------------------------------------------------------

    "SS3 Up arrow" {
        parseAll(byteArrayOf(0x1B, 'O'.code.toByte(), 'A'.code.toByte())) shouldContainExactly
            listOf(key(KeyCode.Up))
    }

    "SS3 Down arrow" {
        parseAll(byteArrayOf(0x1B, 'O'.code.toByte(), 'B'.code.toByte())) shouldContainExactly
            listOf(key(KeyCode.Down))
    }

    "SS3 Left arrow" {
        parseAll(byteArrayOf(0x1B, 'O'.code.toByte(), 'D'.code.toByte())) shouldContainExactly
            listOf(key(KeyCode.Left))
    }

    "SS3 Right arrow" {
        parseAll(byteArrayOf(0x1B, 'O'.code.toByte(), 'C'.code.toByte())) shouldContainExactly
            listOf(key(KeyCode.Right))
    }

    "SS3 Home and End" {
        parseAll("\u001BOH") shouldContainExactly listOf(key(KeyCode.Home))
        parseAll("\u001BOF") shouldContainExactly listOf(key(KeyCode.End))
    }

    "SS3 F1..F4" {
        parseAll("\u001BOP") shouldContainExactly listOf(key(KeyCode.F(1)))
        parseAll("\u001BOQ") shouldContainExactly listOf(key(KeyCode.F(2)))
        parseAll("\u001BOR") shouldContainExactly listOf(key(KeyCode.F(3)))
        parseAll("\u001BOS") shouldContainExactly listOf(key(KeyCode.F(4)))
    }

    // --- CSI arrows / focus / backtab ------------------------------------

    "CSI arrows" {
        parseAll("\u001B[A") shouldContainExactly listOf(key(KeyCode.Up))
        parseAll("\u001B[B") shouldContainExactly listOf(key(KeyCode.Down))
        parseAll("\u001B[C") shouldContainExactly listOf(key(KeyCode.Right))
        parseAll("\u001B[D") shouldContainExactly listOf(key(KeyCode.Left))
    }

    "CSI Home and End" {
        parseAll("\u001B[H") shouldContainExactly listOf(key(KeyCode.Home))
        parseAll("\u001B[F") shouldContainExactly listOf(key(KeyCode.End))
    }

    "CSI BackTab" {
        parseAll("\u001B[Z") shouldContainExactly
            listOf(key(KeyCode.BackTab, KeyModifiers.SHIFT))
    }

    "CSI focus gained / lost" {
        parseAll("\u001B[I") shouldContainExactly listOf(Event.FocusGained)
        parseAll("\u001B[O") shouldContainExactly listOf(Event.FocusLost)
    }

    "CSI shift+left via single-digit modifier (CSI 2 D)" {
        parseAll("\u001B[2D") shouldContainExactly
            listOf(key(KeyCode.Left, KeyModifiers.SHIFT))
    }

    "CSI Ctrl+Left (CSI 1;5 D)" {
        parseAll("\u001B[1;5D") shouldContainExactly
            listOf(key(KeyCode.Left, KeyModifiers.CONTROL))
    }

    "CSI Shift+Up (CSI 1;2 A)" {
        parseAll("\u001B[1;2A") shouldContainExactly
            listOf(key(KeyCode.Up, KeyModifiers.SHIFT))
    }

    "CSI F1 with modifiers (CSI 1;5 P -> Ctrl+F1)" {
        parseAll("\u001B[1;5P") shouldContainExactly
            listOf(key(KeyCode.F(1), KeyModifiers.CONTROL))
    }

    "CSI omitted-1 with kind (CSI ;1:3 B -> Down release)" {
        parseAll("\u001B[;1:3B") shouldContainExactly
            listOf(keyEv(KeyCode.Down, KeyModifiers.NONE, KeyEventKind.Release))
    }

    "CSI explicit-1 with kind (CSI 1;1:3 B -> Down release)" {
        parseAll("\u001B[1;1:3B") shouldContainExactly
            listOf(keyEv(KeyCode.Down, KeyModifiers.NONE, KeyEventKind.Release))
    }

    "CSI bare P -> F1 (kitty legacy short)" {
        parseAll("\u001B[P") shouldContainExactly listOf(key(KeyCode.F(1)))
        parseAll("\u001B[Q") shouldContainExactly listOf(key(KeyCode.F(2)))
        parseAll("\u001B[S") shouldContainExactly listOf(key(KeyCode.F(4)))
    }

    // --- CSI special-keys (~ form) ---------------------------------------

    "CSI special: Insert / Delete / PageUp / PageDown" {
        parseAll("\u001B[2~") shouldContainExactly listOf(key(KeyCode.Insert))
        parseAll("\u001B[3~") shouldContainExactly listOf(key(KeyCode.Delete))
        parseAll("\u001B[5~") shouldContainExactly listOf(key(KeyCode.PageUp))
        parseAll("\u001B[6~") shouldContainExactly listOf(key(KeyCode.PageDown))
    }

    "CSI special: Home aliases (1, 7)" {
        parseAll("\u001B[1~") shouldContainExactly listOf(key(KeyCode.Home))
        parseAll("\u001B[7~") shouldContainExactly listOf(key(KeyCode.Home))
    }

    "CSI special: End aliases (4, 8)" {
        parseAll("\u001B[4~") shouldContainExactly listOf(key(KeyCode.End))
        parseAll("\u001B[8~") shouldContainExactly listOf(key(KeyCode.End))
    }

    "CSI special: F1 (CSI 11~) and F12 (CSI 24~)" {
        parseAll("\u001B[11~") shouldContainExactly listOf(key(KeyCode.F(1)))
        parseAll("\u001B[24~") shouldContainExactly listOf(key(KeyCode.F(12)))
    }

    "CSI special: F5 (CSI 15~) and F10 (CSI 21~)" {
        parseAll("\u001B[15~") shouldContainExactly listOf(key(KeyCode.F(5)))
        parseAll("\u001B[21~") shouldContainExactly listOf(key(KeyCode.F(10)))
    }

    "CSI special key with modifier (CSI 3;2~ -> Shift+Delete)" {
        parseAll("\u001B[3;2~") shouldContainExactly
            listOf(key(KeyCode.Delete, KeyModifiers.SHIFT))
    }

    "CSI special key with kind (CSI 5;1:3~ -> PageUp release)" {
        parseAll("\u001B[5;1:3~") shouldContainExactly
            listOf(keyEv(KeyCode.PageUp, KeyModifiers.NONE, KeyEventKind.Release))
    }

    "CSI special key with mods+kind (CSI 6;5:3~ -> Ctrl+PgDn release)" {
        parseAll("\u001B[6;5:3~") shouldContainExactly
            listOf(keyEv(KeyCode.PageDown, KeyModifiers.CONTROL, KeyEventKind.Release))
    }

    // --- bracketed paste -------------------------------------------------

    "bracketed paste basic" {
        parseAll("\u001B[200~hello world\u001B[201~") shouldContainExactly
            listOf(Event.Paste("hello world"))
    }

    "bracketed paste containing escape sequences is preserved" {
        parseAll("\u001B[200~o\u001B[2D\u001B[201~") shouldContainExactly
            listOf(Event.Paste("o\u001B[2D"))
    }

    "incomplete bracketed paste returns nothing" {
        val p = AnsiInputParser()
        feed(p, "\u001B[200~hello") shouldContainExactly emptyList()
    }

    // --- mouse SGR -------------------------------------------------------

    "mouse SGR Left press" {
        parseAll("\u001B[<0;20;10M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Down(MouseButton.Left), 19, 9)))
    }

    "mouse SGR Left release (lowercase m)" {
        parseAll("\u001B[<0;20;10m") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Up(MouseButton.Left), 19, 9)))
    }

    "mouse SGR drag" {
        parseAll("\u001B[<32;5;6M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Drag(MouseButton.Left), 4, 5)))
    }

    "mouse SGR scroll up" {
        parseAll("\u001B[<64;5;6M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.ScrollUp, 4, 5)))
    }

    "mouse SGR scroll down" {
        parseAll("\u001B[<65;1;1M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.ScrollDown, 0, 0)))
    }

    "mouse SGR with trailing semicolon (Press)" {
        parseAll("\u001B[<0;20;10;M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Down(MouseButton.Left), 19, 9)))
    }

    "mouse SGR with control modifier" {
        parseAll("\u001B[<16;5;5M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Down(MouseButton.Left), 4, 4, KeyModifiers.CONTROL)))
    }

    // --- mouse normal X11 ------------------------------------------------

    "mouse normal X11 Left+Ctrl" {
        parseAll(byteArrayOf(0x1B, '['.code.toByte(), 'M'.code.toByte(), '0'.code.toByte(), 0x60, 0x70)) shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Down(MouseButton.Left), 63, 79, KeyModifiers.CONTROL)))
    }

    // --- mouse RXVT/1015 -------------------------------------------------
    // Cb is the raw button code (no +32 offset), Cx/Cy are 1-indexed.

    "mouse RXVT Left press" {
        parseAll("\u001B[0;20;10M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Down(MouseButton.Left), 19, 9)))
    }

    "mouse RXVT Left release" {
        parseAll("\u001B[3;20;10M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Up(MouseButton.Left), 19, 9)))
    }

    "mouse RXVT motion (no button)" {
        // Cb=35 = button_code 3 (none) | motion_bit 32 → Moved
        parseAll("\u001B[35;252;11M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Moved, 251, 10)))
    }

    "mouse RXVT left drag" {
        // Cb=32 = button_code 0 (left) | motion_bit 32 → Drag(Left)
        parseAll("\u001B[32;5;6M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.Drag(MouseButton.Left), 4, 5)))
    }

    "mouse RXVT scroll up" {
        // Cb=64 = button_code 0 | high_bit 64 → ScrollUp
        parseAll("\u001B[64;5;6M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.ScrollUp, 4, 5)))
    }

    "mouse RXVT scroll down" {
        parseAll("\u001B[65;1;1M") shouldContainExactly
            listOf(Event.Mouse(MouseEvent(MouseEventKind.ScrollDown, 0, 0)))
    }

    // --- kitty CSI-u -----------------------------------------------------

    "kitty CSI-u plain 'a'" {
        parseAll("\u001B[97u") shouldContainExactly listOf(key(KeyCode.Char('a')))
    }

    "kitty CSI-u shift+'a'" {
        parseAll("\u001B[97;2u") shouldContainExactly
            listOf(key(KeyCode.Char('a'), KeyModifiers.SHIFT))
    }

    "kitty CSI-u alt+ctrl+'a'" {
        parseAll("\u001B[97;7u") shouldContainExactly
            listOf(key(KeyCode.Char('a'), KeyModifiers.ALT + KeyModifiers.CONTROL))
    }

    "kitty CSI-u Enter (codepoint 13)" {
        parseAll("\u001B[13u") shouldContainExactly listOf(key(KeyCode.Enter))
    }

    "kitty CSI-u Esc (codepoint 27)" {
        parseAll("\u001B[27u") shouldContainExactly listOf(key(KeyCode.Esc))
    }

    "kitty CSI-u CapsLock (57358)" {
        parseAll("\u001B[57358u") shouldContainExactly listOf(key(KeyCode.CapsLock))
    }

    "kitty CSI-u F13 (57376)" {
        parseAll("\u001B[57376u") shouldContainExactly listOf(key(KeyCode.F(13)))
    }

    "kitty CSI-u media play (57428)" {
        parseAll("\u001B[57428u") shouldContainExactly
            listOf(key(KeyCode.Media(MediaKeyCode.Play)))
    }

    "kitty CSI-u modifier key (57441 -> LeftShift adds SHIFT)" {
        parseAll("\u001B[57441u") shouldContainExactly
            listOf(key(KeyCode.Modifier(ModifierKeyCode.LeftShift), KeyModifiers.SHIFT))
    }

    "kitty CSI-u keypad digit yields KEYPAD state" {
        parseAll("\u001B[57399u") shouldContainExactly
            listOf(keyEv(KeyCode.Char('0'), KeyModifiers.NONE, KeyEventKind.Press, KeyEventState.KEYPAD))
    }

    "kitty CSI-u keypad arrow yields KEYPAD state" {
        parseAll("\u001B[57419u") shouldContainExactly
            listOf(keyEv(KeyCode.Up, KeyModifiers.NONE, KeyEventKind.Press, KeyEventState.KEYPAD))
    }

    "kitty CSI-u with kind (97;1:2 -> Repeat)" {
        parseAll("\u001B[97;1:2u") shouldContainExactly
            listOf(keyEv(KeyCode.Char('a'), KeyModifiers.NONE, KeyEventKind.Repeat))
    }

    "kitty CSI-u with kind (97;1:3 -> Release)" {
        parseAll("\u001B[97;1:3u") shouldContainExactly
            listOf(keyEv(KeyCode.Char('a'), KeyModifiers.NONE, KeyEventKind.Release))
    }

    "kitty CSI-u modifier key with explicit modifier+kind" {
        parseAll("\u001B[57449;3:3u") shouldContainExactly
            listOf(keyEv(KeyCode.Modifier(ModifierKeyCode.RightAlt), KeyModifiers.ALT, KeyEventKind.Release))
    }

    "kitty CSI-u extra-mod SUPER" {
        parseAll("\u001B[97;9u") shouldContainExactly
            listOf(key(KeyCode.Char('a'), KeyModifiers.SUPER))
    }

    "kitty CSI-u extra-mod HYPER" {
        parseAll("\u001B[97;17u") shouldContainExactly
            listOf(key(KeyCode.Char('a'), KeyModifiers.HYPER))
    }

    "kitty CSI-u extra-mod META" {
        parseAll("\u001B[97;33u") shouldContainExactly
            listOf(key(KeyCode.Char('a'), KeyModifiers.META))
    }

    "kitty CSI-u CAPS_LOCK state" {
        parseAll("\u001B[97;65u") shouldContainExactly
            listOf(keyEv(KeyCode.Char('a'), KeyModifiers.NONE, KeyEventKind.Press, KeyEventState.CAPS_LOCK))
    }

    "kitty CSI-u NUM_LOCK state" {
        parseAll("\u001B[49;129u") shouldContainExactly
            listOf(keyEv(KeyCode.Char('1'), KeyModifiers.NONE, KeyEventKind.Press, KeyEventState.NUM_LOCK))
    }

    "kitty CSI-u shifted alternate (A-S-9 -> A-( with shift dropped)" {
        parseAll("\u001B[57:40;4u") shouldContainExactly
            listOf(key(KeyCode.Char('('), KeyModifiers.ALT))
    }

    "kitty CSI-u shifted alternate (A-S-minus -> A-_)" {
        parseAll("\u001B[45:95;4u") shouldContainExactly
            listOf(key(KeyCode.Char('_'), KeyModifiers.ALT))
    }

    // --- UTF-8 -----------------------------------------------------------

    "UTF-8 2-byte char (é = 0xC3 0xA9)" {
        parseAll(byteArrayOf(0xC3.toByte(), 0xA9.toByte())) shouldContainExactly
            listOf(key(KeyCode.Char('é')))
    }

    "UTF-8 3-byte char (€)" {
        parseAll("€") shouldContainExactly listOf(key(KeyCode.Char('€')))
    }

    "UTF-8 multibyte split across advance() calls" {
        val p = AnsiInputParser()
        p.advance(0xC3) shouldBe null
        p.advance(0xA9) shouldBe key(KeyCode.Char('é'))
    }

    // --- split / streaming ---------------------------------------------

    "CSI sequence split byte-by-byte" {
        val p = AnsiInputParser()
        p.advance(0x1B) shouldBe null
        p.advance('['.code) shouldBe null
        p.advance('A'.code) shouldBe key(KeyCode.Up)
    }

    "two CSIs back-to-back" {
        parseAll("\u001B[A\u001B[B") shouldContainExactly
            listOf(key(KeyCode.Up), key(KeyCode.Down))
    }

    "Esc then printable -> Alt+printable" {
        parseAll(byteArrayOf(0x1B, 'x'.code.toByte())) shouldContainExactly
            listOf(key(KeyCode.Char('x'), KeyModifiers.ALT))
    }

    "cursor-position response is silently consumed" {
        // ESC [ 20;10 R should not produce a user-visible event yet.
        parseAll("\u001B[20;10R") shouldContainExactly emptyList()
    }

    "kitty keyboard-flags response is silently consumed" {
        parseAll("\u001B[?1u") shouldContainExactly emptyList()
    }

    "cursor-position response followed by key" {
        parseAll("\u001B[20;10R\u001B[A") shouldContainExactly listOf(key(KeyCode.Up))
    }

    "flush after Esc Esc resolves second Esc" {
        val p = AnsiInputParser()
        p.advance(0x1B) shouldBe null
        // Second 0x1B emits Esc for the first held one and leaves a pending Esc.
        p.advance(0x1B) shouldBe Event.Key(KeyEvent(KeyCode.Esc))
        // Pending Esc resolves on flush.
        p.flush() shouldBe Event.Key(KeyEvent(KeyCode.Esc))
    }
})
