package tools.konsole.core.style

/**
 * A bitset of [Attribute] values. Allocates as a single `Int` (value class).
 *
 * Composes via `+` / `-` operators:
 *
 * ```
 * val style = Attributes.NONE + Attribute.Bold + Attribute.Underlined
 * Attribute.Italic in style                  // false
 * (style - Attribute.Bold) + Attribute.Italic
 * ```
 */
@JvmInline
public value class Attributes internal constructor(public val bits: Int) : Iterable<Attribute> {

    public operator fun contains(a: Attribute): Boolean = bits and (1 shl a.ordinal) != 0
    public operator fun plus(a: Attribute): Attributes = Attributes(bits or (1 shl a.ordinal))
    public operator fun plus(other: Attributes): Attributes = Attributes(bits or other.bits)
    public operator fun minus(a: Attribute): Attributes = Attributes(bits and (1 shl a.ordinal).inv())
    public operator fun minus(other: Attributes): Attributes = Attributes(bits and other.bits.inv())

    public fun isEmpty(): Boolean = bits == 0

    override fun iterator(): Iterator<Attribute> = sequence {
        for (a in Attribute.entries) {
            if (this@Attributes.contains(a)) yield(a)
        }
    }.iterator()

    public companion object {
        public val NONE: Attributes = Attributes(0)

        public fun of(vararg attrs: Attribute): Attributes =
            attrs.fold(NONE) { acc, a -> acc + a }
    }
}

public operator fun Attribute.plus(other: Attribute): Attributes =
    Attributes.of(this, other)
