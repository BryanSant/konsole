package tools.konsole.core.event

/**
 * Bitset of keyboard modifier keys held during a key event.
 *
 * `SUPER`, `HYPER`, and `META` are only delivered when
 * `KeyboardEnhancementFlags.DISAMBIGUATE_ESCAPE_CODES` is active.
 */
@JvmInline
public value class KeyModifiers(public val bits: Int) {

    public operator fun contains(m: KeyModifiers): Boolean = (bits and m.bits) == m.bits
    public operator fun plus(other: KeyModifiers): KeyModifiers = KeyModifiers(bits or other.bits)
    public operator fun minus(other: KeyModifiers): KeyModifiers = KeyModifiers(bits and other.bits.inv())

    public fun isEmpty(): Boolean = bits == 0
    public fun hasShift(): Boolean   = (bits and SHIFT.bits) != 0
    public fun hasControl(): Boolean = (bits and CONTROL.bits) != 0
    public fun hasAlt(): Boolean     = (bits and ALT.bits) != 0
    public fun hasSuper(): Boolean   = (bits and SUPER.bits) != 0
    public fun hasHyper(): Boolean   = (bits and HYPER.bits) != 0
    public fun hasMeta(): Boolean    = (bits and META.bits) != 0

    public companion object {
        public val NONE: KeyModifiers    = KeyModifiers(0)
        public val SHIFT: KeyModifiers   = KeyModifiers(0b00000001)
        public val CONTROL: KeyModifiers = KeyModifiers(0b00000010)
        public val ALT: KeyModifiers     = KeyModifiers(0b00000100)
        public val SUPER: KeyModifiers   = KeyModifiers(0b00001000)
        public val HYPER: KeyModifiers   = KeyModifiers(0b00010000)
        public val META: KeyModifiers    = KeyModifiers(0b00100000)
    }
}
