package tools.konsole.rich

/**
 * Unicode cell-width helpers. Mirrors rich's `cells.py` — most code points
 * are single-cell (ASCII, Latin, common scripts); CJK ideographs, fullwidth
 * forms, emoji, and certain symbols are double-width; combining marks and
 * zero-width formatters are zero-width.
 *
 * Implementation: a small table of double-width code-point ranges
 * (East-Asian Width "W" + "F" + emoji presentation) is consulted; everything
 * else is treated as single-width. This is sufficient for typical terminal
 * content; locale-specific edge cases (East-Asian Width "A" rendered wide
 * under CJK locales) are not modelled.
 */
public object Cells {

    /** Cell width of a single code point. Combining marks return 0. */
    public fun width(codePoint: Int): Int {
        if (codePoint == 0) return 0
        if (codePoint < 0x20 || codePoint in 0x7F..0x9F) return 0 // control
        if (isCombining(codePoint)) return 0
        if (isWide(codePoint)) return 2
        return 1
    }

    /** Cell width of a [Char]. Surrogates are treated as half a wide char (caller should pair them). */
    public fun width(c: Char): Int = width(c.code)

    /**
     * Total cell width of [s], accounting for surrogate pairs, combining
     * marks, and wide characters. ANSI escape sequences are NOT stripped —
     * caller is responsible for not feeding raw SGR text.
     */
    public fun cellLen(s: String): Int {
        if (s.isEmpty()) return 0
        var w = 0
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate()) {
                val cp = Character.toCodePoint(c, s[i + 1])
                w += width(cp)
                i += 2
            } else {
                w += width(c.code)
                i += 1
            }
        }
        return w
    }

    /**
     * Trim or pad [s] to exactly [size] cells. If [s] is wider, it's truncated
     * at the cell boundary (no character split). If narrower, [pad] is appended.
     */
    public fun setCellSize(s: String, size: Int, pad: Char = ' '): String {
        if (size <= 0) return ""
        val current = cellLen(s)
        if (current == size) return s
        if (current < size) return s + pad.toString().repeat(size - current)
        // Truncate
        val sb = StringBuilder()
        var w = 0
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val (cp, step) = if (c.isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate()) {
                Character.toCodePoint(c, s[i + 1]) to 2
            } else {
                c.code to 1
            }
            val cw = width(cp)
            if (w + cw > size) break
            sb.append(s, i, i + step)
            w += cw
            i += step
        }
        // Pad with spaces if we ended on a wide-char boundary that overshot
        while (w < size) {
            sb.append(pad); w++
        }
        return sb.toString()
    }

    private fun isCombining(cp: Int): Boolean {
        // Common combining-mark ranges. Not exhaustive but covers Latin/Greek/Cyrillic combining,
        // diacriticals, and zero-width formatters.
        return cp in 0x0300..0x036F ||      // Combining diacritical marks
            cp in 0x1AB0..0x1AFF ||         // Combining diacritical marks extended
            cp in 0x1DC0..0x1DFF ||         // Combining diacritical marks supplement
            cp in 0x20D0..0x20FF ||         // Combining marks for symbols
            cp in 0xFE20..0xFE2F ||         // Combining half marks
            cp == 0x200B || cp == 0x200C || cp == 0x200D ||  // ZWSP, ZWNJ, ZWJ
            cp == 0xFEFF                    // BOM / ZWNBSP
    }

    private fun isWide(cp: Int): Boolean {
        // East Asian Width Wide + Fullwidth + emoji presentation. Coarse but adequate.
        return cp in 0x1100..0x115F ||      // Hangul Jamo
            cp in 0x231A..0x231B ||         // Watch / hourglass
            cp in 0x2329..0x232A ||         // Angle brackets
            cp in 0x23E9..0x23EC ||         // Media controls
            cp == 0x23F0 || cp == 0x23F3 || // Alarm, hourglass
            cp in 0x25FD..0x25FE ||         // Squares
            cp in 0x2614..0x2615 ||         // Umbrella, hot beverage
            cp in 0x2648..0x2653 ||         // Zodiac
            cp == 0x267F || cp == 0x2693 || cp == 0x26A1 ||
            cp in 0x26AA..0x26AB ||
            cp in 0x26BD..0x26BE ||
            cp in 0x26C4..0x26C5 ||
            cp == 0x26CE || cp == 0x26D4 || cp == 0x26EA ||
            cp in 0x26F2..0x26F3 ||
            cp == 0x26F5 || cp == 0x26FA || cp == 0x26FD ||
            cp == 0x2705 ||
            cp in 0x270A..0x270B ||
            cp == 0x2728 || cp == 0x274C || cp == 0x274E ||
            cp in 0x2753..0x2755 ||
            cp == 0x2757 ||
            cp in 0x2795..0x2797 ||
            cp == 0x27B0 || cp == 0x27BF ||
            cp in 0x2B1B..0x2B1C ||
            cp == 0x2B50 || cp == 0x2B55 ||
            cp in 0x2E80..0x303E ||         // CJK radicals & symbols (excludes Hangul filler)
            cp in 0x3041..0x33FF ||         // Hiragana, Katakana, Bopomofo, CJK
            cp in 0x3400..0x4DBF ||         // CJK Extension A
            cp in 0x4E00..0x9FFF ||         // CJK Unified Ideographs
            cp in 0xA000..0xA4CF ||         // Yi
            cp in 0xAC00..0xD7A3 ||         // Hangul Syllables
            cp in 0xF900..0xFAFF ||         // CJK Compat Ideographs
            cp in 0xFE30..0xFE4F ||         // CJK Compat Forms
            cp in 0xFF00..0xFF60 ||         // Fullwidth ASCII
            cp in 0xFFE0..0xFFE6 ||         // Fullwidth signs
            cp in 0x1F300..0x1F64F ||       // Emoticons + misc symbols & pictographs
            cp in 0x1F680..0x1F6FF ||       // Transport & map symbols
            cp in 0x1F900..0x1F9FF ||       // Supplemental symbols & pictographs
            cp in 0x1FA70..0x1FAFF ||       // Symbols and pictographs extended-A
            cp in 0x20000..0x2FFFD ||       // CJK Extension B-F
            cp in 0x30000..0x3FFFD          // CJK Extension G
    }
}
