package tools.konsole.core.event

/**
 * Bitset of additional state present during a key event. Populated only when
 * Kitty `DISAMBIGUATE_ESCAPE_CODES` is active.
 */
@JvmInline
public value class KeyEventState(public val bits: Int) {

    public operator fun contains(s: KeyEventState): Boolean = (bits and s.bits) == s.bits
    public operator fun plus(other: KeyEventState): KeyEventState = KeyEventState(bits or other.bits)

    public fun isKeypad(): Boolean  = (bits and KEYPAD.bits) != 0
    public fun isCapsLock(): Boolean = (bits and CAPS_LOCK.bits) != 0
    public fun isNumLock(): Boolean  = (bits and NUM_LOCK.bits) != 0

    public companion object {
        public val NONE: KeyEventState     = KeyEventState(0)
        public val KEYPAD: KeyEventState   = KeyEventState(0b001)
        public val CAPS_LOCK: KeyEventState = KeyEventState(0b010)
        public val NUM_LOCK: KeyEventState = KeyEventState(0b100)
    }
}
