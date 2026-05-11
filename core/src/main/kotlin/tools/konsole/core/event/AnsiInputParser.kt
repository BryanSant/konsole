package tools.konsole.core.event

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
import java.io.ByteArrayOutputStream

/**
 * Stateful byte-by-byte ANSI input parser.
 *
 * Feed raw bytes via [advance]; receive completed [Event]s back. The parser
 * holds buffered state across calls so a CSI sequence split across `advance`
 * boundaries is handled correctly.
 *
 * Ported from crossterm `src/event/sys/unix/parse.rs`.
 */
public class AnsiInputParser {

    private val pending = ByteArrayOutputStream(64)

    /**
     * Receives cursor-position responses (`CSI <r>;<c>R`).
     * Set by [tools.konsole.core.event.EventReader] to fulfill
     * [tools.konsole.core.Terminal.cursorPosition] queries. Coordinates are 0-indexed.
     */
    public var onCursorPosition: ((column: Int, row: Int) -> Unit)? = null

    /**
     * Receives Kitty keyboard-flags responses (`CSI ? <flags> u`).
     * Bits match [tools.konsole.core.event.KeyboardEnhancementFlags].
     */
    public var onKeyboardFlags: ((bits: Int) -> Unit)? = null

    /**
     * Feed one byte. Returns:
     *  - non-null `Event` when a complete event was parsed
     *  - null when more bytes are needed
     *
     * Lone `0x1B` is held until [flush] is called or another byte arrives.
     */
    public fun advance(byte: Int): Event? {
        pending.write(byte and 0xFF)
        return tryParse(inputAvailable = true)
    }

    /**
     * Called when no more input is available (e.g. read timeout). Forces a lone
     * pending Esc to resolve to `KeyCode.Esc` and clears any incomplete sequence.
     */
    public fun flush(): Event? {
        if (pending.size() == 0) return null
        val ev = tryParse(inputAvailable = false)
        if (ev == null) {
            // Could not finalize; clear to avoid being stuck.
            pending.reset()
        }
        return ev
    }

    /**
     * Attempt to parse the current pending buffer. Returns null if more bytes are needed
     * (the buffer is left intact); on success, the matched bytes are consumed.
     * On unrecoverable error (malformed sequence), the buffer is dropped and null returned.
     */
    private fun tryParse(inputAvailable: Boolean): Event? {
        // Loop because some sequences are consumed silently (cursor-position responses, etc.)
        // and we want to keep parsing the remaining bytes without dropping anything.
        while (true) {
            if (pending.size() == 0) return null
            val buf = pending.toByteArray()
            when (val result = parseEvent(buf, inputAvailable)) {
                is ParseResult.NeedMore -> return null
                is ParseResult.Failed -> {
                    pending.reset()
                    return null
                }
                is ParseResult.Consumed -> {
                    pending.reset()
                    val remaining = buf.size - result.consumed
                    if (remaining > 0) pending.write(buf, result.consumed, remaining)
                    // Loop to try parsing the remaining bytes.
                    continue
                }
                is ParseResult.Ok -> {
                    val remaining = buf.size - result.consumed
                    pending.reset()
                    if (remaining > 0) {
                        pending.write(buf, result.consumed, remaining)
                    }
                    return result.event
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Parsing primitives
    // ------------------------------------------------------------

    private sealed interface ParseResult {
        data object NeedMore : ParseResult
        data object Failed : ParseResult
        data class Ok(val event: Event, val consumed: Int) : ParseResult
        /** Bytes consumed but no event emitted (e.g. cursor-position response). */
        data class Consumed(val consumed: Int) : ParseResult
    }

    private fun parseEvent(buf: ByteArray, inputAvailable: Boolean): ParseResult {
        if (buf.isEmpty()) return ParseResult.NeedMore

        val b0 = buf[0].toInt() and 0xFF
        return when (b0) {
            0x1B -> parseEsc(buf, inputAvailable)
            0x0D -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Enter)), 1)
            0x09 -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Tab)), 1)
            0x7F, 0x08 -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Backspace)), 1)
            0x00 -> ParseResult.Ok(
                Event.Key(KeyEvent(KeyCode.Char(' '), KeyModifiers.CONTROL)),
                1,
            )
            in 0x01..0x1A -> {
                // Ctrl+a..z. Excludes 0x09 (Tab) and 0x0D (Enter), already handled above.
                val ch = (b0 - 0x01 + 'a'.code).toChar()
                ParseResult.Ok(
                    Event.Key(KeyEvent(KeyCode.Char(ch), KeyModifiers.CONTROL)),
                    1,
                )
            }
            in 0x1C..0x1F -> {
                // Ctrl+4..7
                val ch = (b0 - 0x1C + '4'.code).toChar()
                ParseResult.Ok(
                    Event.Key(KeyEvent(KeyCode.Char(ch), KeyModifiers.CONTROL)),
                    1,
                )
            }
            else -> parseUtf8Char(buf)
        }
    }

    private fun parseEsc(buf: ByteArray, inputAvailable: Boolean): ParseResult {
        // buf[0] == 0x1B
        if (buf.size == 1) {
            return if (inputAvailable) ParseResult.NeedMore
            else ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Esc)), 1)
        }
        val b1 = buf[1].toInt() and 0xFF
        return when (b1) {
            'O'.code -> parseSs3(buf)
            '['.code -> parseCsi(buf)
            ']'.code -> parseOsc(buf)
            0x1B -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Esc)), 1)
            else -> parseEscPrefixed(buf, inputAvailable)
        }
    }

    /**
     * Parse an `ESC ] payload (ST | BEL)` sequence — Operating System Command.
     * Recognises:
     * - `OSC 10 ; rgb:RRRR/GGGG/BBBB ST` — foreground color report → [Event.ForegroundColor]
     * - `OSC 11 ; rgb:RRRR/GGGG/BBBB ST` — background color report → [Event.BackgroundColor]
     * - `OSC 4  ; N ; rgb:RRRR/GGGG/BBBB ST` — palette color report → [Event.PaletteColor]
     *
     * Unknown OSC payloads are silently consumed (return [ParseResult.Consumed]).
     */
    private fun parseOsc(buf: ByteArray): ParseResult {
        // buf starts with ESC ]
        // Find terminator: BEL (0x07) or ST (ESC \). Min payload is empty + terminator.
        var i = 2
        var termLen = 0
        while (i < buf.size) {
            val b = buf[i].toInt() and 0xFF
            if (b == 0x07) { termLen = 1; break }
            if (b == 0x1B && i + 1 < buf.size && (buf[i + 1].toInt() and 0xFF) == '\\'.code) {
                termLen = 2; break
            }
            // ST byte alone (0x9C) — rare on UTF-8 terminals but valid
            if (b == 0x9C) { termLen = 1; break }
            i += 1
        }
        if (termLen == 0) return ParseResult.NeedMore
        val payload = String(buf, 2, i - 2, Charsets.US_ASCII)
        val consumed = i + termLen
        val parts = payload.split(';')
        return when (val tag = parts.getOrNull(0)?.toIntOrNull()) {
            10 -> parseOscColor(parts, consumed) { r, g, b -> Event.ForegroundColor(r, g, b) }
            11 -> parseOscColor(parts, consumed) { r, g, b -> Event.BackgroundColor(r, g, b) }
            4 -> parseOscPaletteColor(parts, consumed)
            null -> ParseResult.Consumed(consumed)
            else -> {
                // Recognised tag but we don't render its payload — drop silently.
                @Suppress("UNUSED_VARIABLE") val _t = tag
                ParseResult.Consumed(consumed)
            }
        }
    }

    private inline fun parseOscColor(parts: List<String>, consumed: Int, build: (Int, Int, Int) -> Event): ParseResult {
        // parts[0] = "10" or "11"; parts[1] = "rgb:RRRR/GGGG/BBBB"
        val rgb = parseRgbToken(parts.getOrNull(1) ?: return ParseResult.Consumed(consumed))
            ?: return ParseResult.Consumed(consumed)
        return ParseResult.Ok(build(rgb.first, rgb.second, rgb.third), consumed)
    }

    private fun parseOscPaletteColor(parts: List<String>, consumed: Int): ParseResult {
        // parts[0] = "4"; parts[1] = index; parts[2] = "rgb:RRRR/GGGG/BBBB"
        val idx = parts.getOrNull(1)?.toIntOrNull() ?: return ParseResult.Consumed(consumed)
        val rgb = parseRgbToken(parts.getOrNull(2) ?: return ParseResult.Consumed(consumed))
            ?: return ParseResult.Consumed(consumed)
        return ParseResult.Ok(Event.PaletteColor(idx, rgb.first, rgb.second, rgb.third), consumed)
    }

    /**
     * Parse `rgb:RRRR/GGGG/BBBB` (X11 color-spec format used by xterm). Components
     * can be 1-4 hex digits; we scale each to 8-bit by taking the high byte.
     */
    private fun parseRgbToken(token: String): Triple<Int, Int, Int>? {
        if (!token.startsWith("rgb:")) return null
        val body = token.removePrefix("rgb:")
        val parts = body.split('/')
        if (parts.size != 3) return null
        fun scale(hex: String): Int? {
            if (hex.isEmpty() || hex.length > 4) return null
            val v = hex.toIntOrNull(16) ?: return null
            // Scale to 8-bit: 1-digit → *17, 2-digit → as-is, 3-digit → >>4, 4-digit → >>8
            return when (hex.length) {
                1 -> v * 17
                2 -> v
                3 -> v shr 4
                4 -> v shr 8
                else -> v
            }.coerceIn(0, 255)
        }
        val r = scale(parts[0]) ?: return null
        val g = scale(parts[1]) ?: return null
        val b = scale(parts[2]) ?: return null
        return Triple(r, g, b)
    }

    private fun parseSs3(buf: ByteArray): ParseResult {
        // buf starts with ESC O
        if (buf.size == 2) return ParseResult.NeedMore
        val b = buf[2].toInt() and 0xFF
        val key = when (b) {
            'D'.code -> KeyCode.Left
            'C'.code -> KeyCode.Right
            'A'.code -> KeyCode.Up
            'B'.code -> KeyCode.Down
            'H'.code -> KeyCode.Home
            'F'.code -> KeyCode.End
            in 'P'.code..'S'.code -> KeyCode.F(1 + (b - 'P'.code))
            else -> return ParseResult.Failed
        }
        return ParseResult.Ok(Event.Key(KeyEvent(key)), 3)
    }

    /**
     * Esc followed by something that's not '[' or 'O' or another ESC. The Rust impl
     * recursively parses buf[1..] and ORs ALT into the resulting key event modifier.
     */
    private fun parseEscPrefixed(buf: ByteArray, inputAvailable: Boolean): ParseResult {
        val sub = buf.copyOfRange(1, buf.size)
        return when (val inner = parseEvent(sub, inputAvailable)) {
            is ParseResult.NeedMore -> ParseResult.NeedMore
            is ParseResult.Failed -> ParseResult.Failed
            is ParseResult.Consumed -> ParseResult.Consumed(inner.consumed + 1)
            is ParseResult.Ok -> {
                val newEv = when (val ev = inner.event) {
                    is Event.Key -> Event.Key(
                        ev.event.copy(modifiers = ev.event.modifiers + KeyModifiers.ALT),
                    )
                    else -> ev
                }
                ParseResult.Ok(newEv, inner.consumed + 1)
            }
        }
    }

    // ---- CSI -------------------------------------------------------

    private fun parseCsi(buf: ByteArray): ParseResult {
        // buf starts with ESC [
        if (buf.size == 2) return ParseResult.NeedMore

        val b2 = buf[2].toInt() and 0xFF
        return when (b2) {
            '['.code -> {
                // ESC [ [ <A..E> -> F1..F5 (linux console)
                if (buf.size == 3) ParseResult.NeedMore
                else {
                    val v = buf[3].toInt() and 0xFF
                    if (v in 'A'.code..'E'.code) {
                        ParseResult.Ok(Event.Key(KeyEvent(KeyCode.F(1 + (v - 'A'.code)))), 4)
                    } else ParseResult.Failed
                }
            }
            'D'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Left)), 3)
            'C'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Right)), 3)
            'A'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Up)), 3)
            'B'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Down)), 3)
            'H'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Home)), 3)
            'F'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.End)), 3)
            'Z'.code -> ParseResult.Ok(
                Event.Key(KeyEvent(KeyCode.BackTab, KeyModifiers.SHIFT)),
                3,
            )
            'M'.code -> parseCsiNormalMouse(buf)
            '<'.code -> parseCsiSgrMouse(buf)
            'I'.code -> ParseResult.Ok(Event.FocusGained, 3)
            'O'.code -> ParseResult.Ok(Event.FocusLost, 3)
            ';'.code -> {
                // CSI ; ...  modifier-key form with omitted leading 1
                val end = findCsiFinalByte(buf, 2) ?: return ParseResult.NeedMore
                parseCsiModifierKeyCode(buf, end)
            }
            'P'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.F(1))), 3)
            'Q'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.F(2))), 3)
            'S'.code -> ParseResult.Ok(Event.Key(KeyEvent(KeyCode.F(4))), 3)
            '?'.code -> {
                // Kitty keyboard flags (?...u), device attrs (?...c), or DECRPM (?...$y).
                val end = findCsiFinalByte(buf, 2) ?: return ParseResult.NeedMore
                val finalByte = buf[end].toInt() and 0xFF
                when (finalByte) {
                    'u'.code -> {
                        // CSI ? flags u — fire the flags listener if present.
                        val params = String(buf, 3, end - 3, Charsets.US_ASCII)
                        val bits = params.toIntOrNull() ?: 0
                        onKeyboardFlags?.invoke(bits)
                        return ParseResult.Consumed(end + 1)
                    }
                    'c'.code -> return ParseResult.Consumed(end + 1)   // primary device attrs
                    'y'.code -> {
                        // DECRPM: CSI ? <mode> ; <state> $ y. The "$" is the byte immediately before "y".
                        if (end >= 1 && (buf[end - 1].toInt() and 0xFF) == '$'.code) {
                            val params = String(buf, 3, end - 1 - 3, Charsets.US_ASCII)
                            val parts = params.split(';')
                            val mode = parts.getOrNull(0)?.toIntOrNull()
                            val state = parts.getOrNull(1)?.toIntOrNull()
                            if (mode != null && state != null) {
                                return ParseResult.Ok(Event.ModeReport(mode, ModeState.fromCode(state)), end + 1)
                            }
                        }
                        return ParseResult.Consumed(end + 1)
                    }
                    else -> return ParseResult.Failed
                }
            }
            in '0'.code..'9'.code -> {
                if (buf.size == 3) return ParseResult.NeedMore
                // Bracketed paste opens with ESC [ 2 0 0 ~. Detect early.
                if (buf.size >= 6 && startsWith(buf, BRACKETED_PASTE_OPEN)) {
                    return parseCsiBracketedPaste(buf)
                }
                val end = findCsiFinalByte(buf, 2) ?: return ParseResult.NeedMore
                val finalByte = buf[end].toInt() and 0xFF
                when (finalByte) {
                    'M'.code -> parseCsiRxvtMouse(buf, end)
                    '~'.code -> parseCsiSpecialKeyCode(buf, end)
                    'u'.code -> parseCsiUEncodedKeyCode(buf, end)
                    't'.code -> parseCsiWindowOp(buf, end)
                    'R'.code -> {
                        // CSI <row>;<col> R — cursor-position response. Reported coordinates
                        // are 1-indexed; we surface them 0-indexed to match the rest of the
                        // public API (MoveTo, MouseEvent, etc.).
                        val params = String(buf, 2, end - 2, Charsets.US_ASCII)
                        val parts = params.split(';')
                        val row = parts.getOrNull(0)?.toIntOrNull()?.minus(1)?.coerceAtLeast(0) ?: 0
                        val col = parts.getOrNull(1)?.toIntOrNull()?.minus(1)?.coerceAtLeast(0) ?: 0
                        onCursorPosition?.invoke(col, row)
                        ParseResult.Consumed(end + 1)
                    }
                    else -> parseCsiModifierKeyCode(buf, end)
                }
            }
            else -> ParseResult.Failed
        }
    }


    /**
     * Parse `CSI Ps ; Ps ; … t` — xterm window-manipulation reply.
     *
     * Of these, konsole only surfaces the in-band resize report:
     * `CSI 48 ; rows ; cols ; pixHeight ; pixWidth t` (xterm mode 2048 in-band
     * window-size notifications). All other window-op replies (text-only, icon,
     * etc.) are consumed silently.
     */
    private fun parseCsiWindowOp(buf: ByteArray, finalIdx: Int): ParseResult {
        // buf starts with ESC [, ends with 't' at finalIdx
        val payload = String(buf, 2, finalIdx - 2, Charsets.US_ASCII)
        val parts = payload.split(';')
        val op = parts.getOrNull(0)?.toIntOrNull() ?: return ParseResult.Consumed(finalIdx + 1)
        if (op == 48 && parts.size >= 3) {
            val rows = parts[1].toIntOrNull()
            val cols = parts[2].toIntOrNull()
            if (rows != null && cols != null) {
                return ParseResult.Ok(Event.Resize(cols, rows), finalIdx + 1)
            }
        }
        // Other window ops not currently surfaced as events.
        return ParseResult.Consumed(finalIdx + 1)
    }

    // ---- CSI special-key (~) ---------------------------------------

    private fun parseCsiSpecialKeyCode(buf: ByteArray, finalIdx: Int): ParseResult {
        // buf starts with ESC [, ends with ~ at finalIdx
        val payload = String(buf, 2, finalIdx - 2, Charsets.US_ASCII)
        val parts = payload.split(';')
        val first = parts[0].toIntOrNull() ?: return ParseResult.Failed

        var modifiers = KeyModifiers.NONE
        var kind = KeyEventKind.Press
        var state = KeyEventState.NONE
        if (parts.size > 1) {
            val (mods, knd, st) = parseModifierAndKindField(parts[1])
            modifiers = mods
            kind = knd
            state = st
        }

        val code: KeyCode = when (first) {
            1, 7 -> KeyCode.Home
            2 -> KeyCode.Insert
            3 -> KeyCode.Delete
            4, 8 -> KeyCode.End
            5 -> KeyCode.PageUp
            6 -> KeyCode.PageDown
            in 11..15 -> KeyCode.F(first - 10)
            in 17..21 -> KeyCode.F(first - 11)
            in 23..26 -> KeyCode.F(first - 12)
            in 28..29 -> KeyCode.F(first - 15)
            in 31..34 -> KeyCode.F(first - 17)
            else -> return ParseResult.Failed
        }
        return ParseResult.Ok(
            Event.Key(KeyEvent(code, modifiers, kind, state)),
            finalIdx + 1,
        )
    }

    // ---- CSI modifier-key form (final byte is letter) --------------

    private fun parseCsiModifierKeyCode(buf: ByteArray, finalIdx: Int): ParseResult {
        // buf: ESC [ <params> <letter>
        val finalByte = buf[finalIdx].toInt() and 0xFF
        val payload = String(buf, 2, finalIdx - 2, Charsets.US_ASCII)
        val parts = payload.split(';')

        var modifiers = KeyModifiers.NONE
        var kind = KeyEventKind.Press

        // Common form: "<first>;<mods>[:<kind>]<letter>"
        if (parts.size > 1) {
            val (m, k, _) = parseModifierAndKindField(parts[1])
            modifiers = m
            kind = k
        } else if (parts.size == 1 && parts[0].isNotEmpty()) {
            // Form: "ESC [ <digit> <letter>" — single digit is the modifier mask.
            val maybe = parts[0].toIntOrNull()
            if (maybe != null) {
                modifiers = decodeModifiers(maybe)
            }
        }

        val code = when (finalByte) {
            'A'.code -> KeyCode.Up
            'B'.code -> KeyCode.Down
            'C'.code -> KeyCode.Right
            'D'.code -> KeyCode.Left
            'F'.code -> KeyCode.End
            'H'.code -> KeyCode.Home
            'P'.code -> KeyCode.F(1)
            'Q'.code -> KeyCode.F(2)
            'R'.code -> KeyCode.F(3)
            'S'.code -> KeyCode.F(4)
            else -> return ParseResult.Failed
        }
        return ParseResult.Ok(
            Event.Key(KeyEvent(code, modifiers, kind, KeyEventState.NONE)),
            finalIdx + 1,
        )
    }

    // ---- CSI u (kitty / fixterms) ----------------------------------

    private fun parseCsiUEncodedKeyCode(buf: ByteArray, finalIdx: Int): ParseResult {
        val payload = String(buf, 2, finalIdx - 2, Charsets.US_ASCII)
        val parts = payload.split(';')

        val codepointParts = parts[0].split(':')
        val codepoint = codepointParts[0].toIntOrNull() ?: return ParseResult.Failed

        var modifiers = KeyModifiers.NONE
        var kind = KeyEventKind.Press
        var stateFromMods = KeyEventState.NONE
        if (parts.size > 1) {
            val (m, k, s) = parseModifierAndKindField(parts[1])
            modifiers = m
            kind = k
            stateFromMods = s
        }

        var stateFromKey = KeyEventState.NONE
        var keycode: KeyCode = run {
            val translated = translateFunctionalKeyCode(codepoint)
            if (translated != null) {
                stateFromKey = translated.second
                translated.first
            } else if (codepoint in 0..0x10FFFF) {
                val c = codepoint.toChar()
                when (codepoint) {
                    0x1B -> KeyCode.Esc
                    0x0D -> KeyCode.Enter
                    0x09 -> if (modifiers.hasShift()) KeyCode.BackTab else KeyCode.Tab
                    0x7F -> KeyCode.Backspace
                    else -> KeyCode.Char(c)
                }
            } else return ParseResult.Failed
        }

        // If the keycode is a Modifier itself, OR the corresponding bit into modifiers.
        if (keycode is KeyCode.Modifier) {
            modifiers = when (keycode.code) {
                ModifierKeyCode.LeftAlt, ModifierKeyCode.RightAlt -> modifiers + KeyModifiers.ALT
                ModifierKeyCode.LeftControl, ModifierKeyCode.RightControl -> modifiers + KeyModifiers.CONTROL
                ModifierKeyCode.LeftShift, ModifierKeyCode.RightShift -> modifiers + KeyModifiers.SHIFT
                ModifierKeyCode.LeftSuper, ModifierKeyCode.RightSuper -> modifiers + KeyModifiers.SUPER
                ModifierKeyCode.LeftHyper, ModifierKeyCode.RightHyper -> modifiers + KeyModifiers.HYPER
                ModifierKeyCode.LeftMeta, ModifierKeyCode.RightMeta -> modifiers + KeyModifiers.META
                else -> modifiers
            }
        }

        // Shifted alt-keycode (kitty REPORT_ALTERNATE_KEYS): if shift is set and a second
        // codepoint is present after ':', use it as the keycode and drop SHIFT.
        if (modifiers.hasShift() && codepointParts.size > 1) {
            val shifted = codepointParts[1].toIntOrNull()
            if (shifted != null && shifted in 0..0x10FFFF) {
                keycode = KeyCode.Char(shifted.toChar())
                modifiers -= KeyModifiers.SHIFT
            }
        }

        return ParseResult.Ok(
            Event.Key(KeyEvent(keycode, modifiers, kind, stateFromKey + stateFromMods)),
            finalIdx + 1,
        )
    }

    // ---- mouse ------------------------------------------------------

    private fun parseCsiNormalMouse(buf: ByteArray): ParseResult {
        // ESC [ M Cb Cx Cy
        if (buf.size < 6) return ParseResult.NeedMore
        val cb = (buf[3].toInt() and 0xFF) - 32
        if (cb < 0) return ParseResult.Failed
        val (kind, modifiers) = parseCb(cb) ?: return ParseResult.Failed
        val cx = ((buf[4].toInt() and 0xFF) - 32 - 1).coerceAtLeast(0)
        val cy = ((buf[5].toInt() and 0xFF) - 32 - 1).coerceAtLeast(0)
        return ParseResult.Ok(Event.Mouse(MouseEvent(kind, cx, cy, modifiers)), 6)
    }

    private fun parseCsiRxvtMouse(buf: ByteArray, finalIdx: Int): ParseResult {
        // ESC [ Cb ; Cx ; Cy M  — final byte 'M' at finalIdx
        // RXVT/1015 sends Cb as the raw button code (no +32 offset), unlike X10 normal.
        val payload = String(buf, 2, finalIdx - 2, Charsets.US_ASCII)
        val parts = payload.split(';').filter { it.isNotEmpty() }
        if (parts.size < 3) return ParseResult.Failed
        val cb = parts[0].toIntOrNull() ?: return ParseResult.Failed
        if (cb < 0) return ParseResult.Failed
        val (kind, modifiers) = parseCb(cb) ?: return ParseResult.Failed
        val cx = (parts[1].toIntOrNull() ?: return ParseResult.Failed) - 1
        val cy = (parts[2].toIntOrNull() ?: return ParseResult.Failed) - 1
        return ParseResult.Ok(Event.Mouse(MouseEvent(kind, cx, cy, modifiers)), finalIdx + 1)
    }

    private fun parseCsiSgrMouse(buf: ByteArray): ParseResult {
        // ESC [ < Cb ; Cx ; Cy [;] M_or_m
        if (buf.size < 4) return ParseResult.NeedMore
        // Search for terminator 'M' or 'm' from byte 3.
        var idx = 3
        while (idx < buf.size) {
            val b = buf[idx].toInt() and 0xFF
            if (b == 'M'.code || b == 'm'.code) break
            idx++
        }
        if (idx >= buf.size) return ParseResult.NeedMore
        val finalByte = buf[idx].toInt() and 0xFF
        // payload is buf[3..idx)
        val payload = String(buf, 3, idx - 3, Charsets.US_ASCII)
        val parts = payload.split(';').filter { it.isNotEmpty() }
        if (parts.size < 3) return ParseResult.Failed
        val cb = parts[0].toIntOrNull() ?: return ParseResult.Failed
        val (kindRaw, modifiers) = parseCb(cb) ?: return ParseResult.Failed
        val cx = (parts[1].toIntOrNull() ?: return ParseResult.Failed) - 1
        val cy = (parts[2].toIntOrNull() ?: return ParseResult.Failed) - 1
        // Lowercase 'm' = release: convert Down -> Up.
        val kind = if (finalByte == 'm'.code && kindRaw is MouseEventKind.Down) {
            MouseEventKind.Up(kindRaw.button)
        } else kindRaw
        return ParseResult.Ok(Event.Mouse(MouseEvent(kind, cx, cy, modifiers)), idx + 1)
    }

    private fun parseCb(cb: Int): Pair<MouseEventKind, KeyModifiers>? {
        val buttonNumber = (cb and 0b0000_0011) or ((cb and 0b1100_0000) ushr 4)
        val dragging = (cb and 0b0010_0000) != 0
        val kind: MouseEventKind = when (buttonNumber to dragging) {
            0 to false -> MouseEventKind.Down(MouseButton.Left)
            1 to false -> MouseEventKind.Down(MouseButton.Middle)
            2 to false -> MouseEventKind.Down(MouseButton.Right)
            0 to true -> MouseEventKind.Drag(MouseButton.Left)
            1 to true -> MouseEventKind.Drag(MouseButton.Middle)
            2 to true -> MouseEventKind.Drag(MouseButton.Right)
            3 to false -> MouseEventKind.Up(MouseButton.Left)
            3 to true, 4 to true, 5 to true -> MouseEventKind.Moved
            4 to false -> MouseEventKind.ScrollUp
            5 to false -> MouseEventKind.ScrollDown
            6 to false -> MouseEventKind.ScrollLeft
            7 to false -> MouseEventKind.ScrollRight
            else -> return null
        }
        var modifiers = KeyModifiers.NONE
        if ((cb and 0b0000_0100) != 0) modifiers += KeyModifiers.SHIFT
        if ((cb and 0b0000_1000) != 0) modifiers += KeyModifiers.ALT
        if ((cb and 0b0001_0000) != 0) modifiers += KeyModifiers.CONTROL
        return kind to modifiers
    }

    // ---- bracketed paste -------------------------------------------

    private fun parseCsiBracketedPaste(buf: ByteArray): ParseResult {
        // buf starts with ESC [ 2 0 0 ~ ; look for ESC [ 2 0 1 ~
        val needleIdx = indexOf(buf, BRACKETED_PASTE_CLOSE, 6)
        if (needleIdx < 0) return ParseResult.NeedMore
        val text = String(
            buf,
            BRACKETED_PASTE_OPEN.size,
            needleIdx - BRACKETED_PASTE_OPEN.size,
            Charsets.UTF_8,
        )
        return ParseResult.Ok(
            Event.Paste(text),
            needleIdx + BRACKETED_PASTE_CLOSE.size,
        )
    }

    // ---- UTF-8 ------------------------------------------------------

    private fun parseUtf8Char(buf: ByteArray): ParseResult {
        val b0 = buf[0].toInt() and 0xFF
        // ASCII printable
        if (b0 in 0x20..0x7E) {
            val c = b0.toChar()
            val mods = if (c.isUpperCase()) KeyModifiers.SHIFT else KeyModifiers.NONE
            return ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Char(c), mods)), 1)
        }
        val required = when (b0) {
            in 0xC0..0xDF -> 2
            in 0xE0..0xEF -> 3
            in 0xF0..0xF7 -> 4
            else -> return ParseResult.Failed
        }
        if (buf.size < required) {
            // Validate continuation bytes we already have.
            for (i in 1 until buf.size) {
                if ((buf[i].toInt() and 0xC0) != 0x80) return ParseResult.Failed
            }
            return ParseResult.NeedMore
        }
        // Try decoding required bytes.
        val sub = ByteArray(required) { buf[it] }
        return try {
            val s = String(sub, Charsets.UTF_8)
            if (s.isEmpty()) ParseResult.Failed
            else {
                val ch = s[0]
                val mods = if (ch.isUpperCase()) KeyModifiers.SHIFT else KeyModifiers.NONE
                ParseResult.Ok(Event.Key(KeyEvent(KeyCode.Char(ch), mods)), required)
            }
        } catch (_: Exception) {
            ParseResult.Failed
        }
    }

    // ---- helpers ----------------------------------------------------

    /**
     * Decode the kitty-style modifier param "<mask>[:<kind>]" — returns
     * (modifiers, kind, state-from-modifier-bits).
     */
    private fun parseModifierAndKindField(field: String): Triple<KeyModifiers, KeyEventKind, KeyEventState> {
        if (field.isEmpty()) return Triple(KeyModifiers.NONE, KeyEventKind.Press, KeyEventState.NONE)
        val sub = field.split(':')
        val mask = sub[0].toIntOrNull() ?: return Triple(KeyModifiers.NONE, KeyEventKind.Press, KeyEventState.NONE)
        val kindCode = if (sub.size > 1) sub[1].toIntOrNull() ?: 1 else 1
        return Triple(decodeModifiers(mask), decodeKind(kindCode), decodeStateFromModifiers(mask))
    }

    private fun decodeModifiers(mask: Int): KeyModifiers {
        val bits = (mask - 1).coerceAtLeast(0)
        var m = KeyModifiers.NONE
        if ((bits and 1) != 0) m += KeyModifiers.SHIFT
        if ((bits and 2) != 0) m += KeyModifiers.ALT
        if ((bits and 4) != 0) m += KeyModifiers.CONTROL
        if ((bits and 8) != 0) m += KeyModifiers.SUPER
        if ((bits and 16) != 0) m += KeyModifiers.HYPER
        if ((bits and 32) != 0) m += KeyModifiers.META
        return m
    }

    private fun decodeStateFromModifiers(mask: Int): KeyEventState {
        val bits = (mask - 1).coerceAtLeast(0)
        var s = KeyEventState.NONE
        if ((bits and 64) != 0) s += KeyEventState.CAPS_LOCK
        if ((bits and 128) != 0) s += KeyEventState.NUM_LOCK
        return s
    }

    private fun decodeKind(code: Int): KeyEventKind = when (code) {
        1 -> KeyEventKind.Press
        2 -> KeyEventKind.Repeat
        3 -> KeyEventKind.Release
        else -> KeyEventKind.Press
    }

    /**
     * Find the final-byte index for a CSI sequence starting at `start` (i.e. the first
     * byte after `ESC [`). The final byte is anything in 0x40..0x7E. Returns null
     * if not yet seen (need more bytes).
     */
    private fun findCsiFinalByte(buf: ByteArray, start: Int): Int? {
        var i = start
        while (i < buf.size) {
            val b = buf[i].toInt() and 0xFF
            if (b in 0x40..0x7E) return i
            i++
        }
        return null
    }

    private fun startsWith(buf: ByteArray, prefix: ByteArray): Boolean {
        if (buf.size < prefix.size) return false
        for (i in prefix.indices) if (buf[i] != prefix[i]) return false
        return true
    }

    private fun indexOf(buf: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty()) return from
        var i = from
        val end = buf.size - needle.size
        while (i <= end) {
            var j = 0
            while (j < needle.size && buf[i + j] == needle[j]) j++
            if (j == needle.size) return i
            i++
        }
        return -1
    }

    private fun translateFunctionalKeyCode(codepoint: Int): Pair<KeyCode, KeyEventState>? {
        // Keypad codes -> code with KEYPAD state.
        val keypad: KeyCode? = when (codepoint) {
            57399 -> KeyCode.Char('0')
            57400 -> KeyCode.Char('1')
            57401 -> KeyCode.Char('2')
            57402 -> KeyCode.Char('3')
            57403 -> KeyCode.Char('4')
            57404 -> KeyCode.Char('5')
            57405 -> KeyCode.Char('6')
            57406 -> KeyCode.Char('7')
            57407 -> KeyCode.Char('8')
            57408 -> KeyCode.Char('9')
            57409 -> KeyCode.Char('.')
            57410 -> KeyCode.Char('/')
            57411 -> KeyCode.Char('*')
            57412 -> KeyCode.Char('-')
            57413 -> KeyCode.Char('+')
            57414 -> KeyCode.Enter
            57415 -> KeyCode.Char('=')
            57416 -> KeyCode.Char(',')
            57417 -> KeyCode.Left
            57418 -> KeyCode.Right
            57419 -> KeyCode.Up
            57420 -> KeyCode.Down
            57421 -> KeyCode.PageUp
            57422 -> KeyCode.PageDown
            57423 -> KeyCode.Home
            57424 -> KeyCode.End
            57425 -> KeyCode.Insert
            57426 -> KeyCode.Delete
            57427 -> KeyCode.KeypadBegin
            else -> null
        }
        if (keypad != null) return keypad to KeyEventState.KEYPAD

        val other: KeyCode? = when (codepoint) {
            57358 -> KeyCode.CapsLock
            57359 -> KeyCode.ScrollLock
            57360 -> KeyCode.NumLock
            57361 -> KeyCode.PrintScreen
            57362 -> KeyCode.Pause
            57363 -> KeyCode.Menu
            in 57376..57398 -> KeyCode.F(13 + (codepoint - 57376))
            57428 -> KeyCode.Media(MediaKeyCode.Play)
            57429 -> KeyCode.Media(MediaKeyCode.Pause)
            57430 -> KeyCode.Media(MediaKeyCode.PlayPause)
            57431 -> KeyCode.Media(MediaKeyCode.Reverse)
            57432 -> KeyCode.Media(MediaKeyCode.Stop)
            57433 -> KeyCode.Media(MediaKeyCode.FastForward)
            57434 -> KeyCode.Media(MediaKeyCode.Rewind)
            57435 -> KeyCode.Media(MediaKeyCode.TrackNext)
            57436 -> KeyCode.Media(MediaKeyCode.TrackPrevious)
            57437 -> KeyCode.Media(MediaKeyCode.Record)
            57438 -> KeyCode.Media(MediaKeyCode.LowerVolume)
            57439 -> KeyCode.Media(MediaKeyCode.RaiseVolume)
            57440 -> KeyCode.Media(MediaKeyCode.MuteVolume)
            57441 -> KeyCode.Modifier(ModifierKeyCode.LeftShift)
            57442 -> KeyCode.Modifier(ModifierKeyCode.LeftControl)
            57443 -> KeyCode.Modifier(ModifierKeyCode.LeftAlt)
            57444 -> KeyCode.Modifier(ModifierKeyCode.LeftSuper)
            57445 -> KeyCode.Modifier(ModifierKeyCode.LeftHyper)
            57446 -> KeyCode.Modifier(ModifierKeyCode.LeftMeta)
            57447 -> KeyCode.Modifier(ModifierKeyCode.RightShift)
            57448 -> KeyCode.Modifier(ModifierKeyCode.RightControl)
            57449 -> KeyCode.Modifier(ModifierKeyCode.RightAlt)
            57450 -> KeyCode.Modifier(ModifierKeyCode.RightSuper)
            57451 -> KeyCode.Modifier(ModifierKeyCode.RightHyper)
            57452 -> KeyCode.Modifier(ModifierKeyCode.RightMeta)
            57453 -> KeyCode.Modifier(ModifierKeyCode.IsoLevel3Shift)
            57454 -> KeyCode.Modifier(ModifierKeyCode.IsoLevel5Shift)
            else -> null
        }
        return if (other != null) other to KeyEventState.NONE else null
    }

    private companion object {
        // ESC [ 2 0 0 ~
        private val BRACKETED_PASTE_OPEN: ByteArray = byteArrayOf(
            0x1B, '['.code.toByte(), '2'.code.toByte(),
            '0'.code.toByte(), '0'.code.toByte(), '~'.code.toByte(),
        )
        // ESC [ 2 0 1 ~
        private val BRACKETED_PASTE_CLOSE: ByteArray = byteArrayOf(
            0x1B, '['.code.toByte(), '2'.code.toByte(),
            '0'.code.toByte(), '1'.code.toByte(), '~'.code.toByte(),
        )
    }
}
