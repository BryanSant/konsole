package tools.konsole.core.event

/**
 * Bitset of [Kitty keyboard protocol](https://sw.kovidgoyal.net/kitty/keyboard-protocol/)
 * progressive-enhancement flags.
 *
 * Sent via [PushKeyboardEnhancementFlags] / [PopKeyboardEnhancementFlags] commands.
 */
@JvmInline
public value class KeyboardEnhancementFlags(public val bits: Int) {

    public operator fun contains(o: KeyboardEnhancementFlags): Boolean =
        (bits and o.bits) == o.bits

    public operator fun plus(o: KeyboardEnhancementFlags): KeyboardEnhancementFlags =
        KeyboardEnhancementFlags(bits or o.bits)

    public operator fun minus(o: KeyboardEnhancementFlags): KeyboardEnhancementFlags =
        KeyboardEnhancementFlags(bits and o.bits.inv())

    public companion object {
        public val NONE                            : KeyboardEnhancementFlags = KeyboardEnhancementFlags(0)
        public val DISAMBIGUATE_ESCAPE_CODES       : KeyboardEnhancementFlags = KeyboardEnhancementFlags(0b00001)
        public val REPORT_EVENT_TYPES              : KeyboardEnhancementFlags = KeyboardEnhancementFlags(0b00010)
        public val REPORT_ALTERNATE_KEYS           : KeyboardEnhancementFlags = KeyboardEnhancementFlags(0b00100)
        public val REPORT_ALL_KEYS_AS_ESCAPE_CODES : KeyboardEnhancementFlags = KeyboardEnhancementFlags(0b01000)
        public val REPORT_ASSOCIATED_TEXT          : KeyboardEnhancementFlags = KeyboardEnhancementFlags(0b10000)

        public val ALL: KeyboardEnhancementFlags =
            DISAMBIGUATE_ESCAPE_CODES + REPORT_EVENT_TYPES + REPORT_ALTERNATE_KEYS +
                REPORT_ALL_KEYS_AS_ESCAPE_CODES + REPORT_ASSOCIATED_TEXT
    }
}
