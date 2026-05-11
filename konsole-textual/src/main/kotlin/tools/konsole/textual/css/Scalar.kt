package tools.konsole.textual.css

/**
 * A length value in a TCSS declaration — `40`, `50%`, `1fr`, `auto`.
 * Mirrors Python textual's `scalar.Scalar`.
 *
 * Resolution to concrete cell counts is delegated to layout passes which know
 * the parent's content dimension and total fractional units.
 */
public data class Scalar(public val value: Double, public val unit: Unit) {

    /** Resolve to absolute cells given parent [containerSize] and (for fr units) the [totalFractions]. */
    public fun resolve(containerSize: Int, totalFractions: Double = 1.0): Int = when (unit) {
        Unit.Cells -> value.toInt()
        Unit.Percent -> ((value / 100.0) * containerSize).toInt()
        Unit.Fraction -> if (totalFractions <= 0.0) 0 else ((value / totalFractions) * containerSize).toInt()
        Unit.Auto -> -1 // sentinel — layout pass decides
    }

    public val isAuto: Boolean get() = unit == Unit.Auto
    public val isFraction: Boolean get() = unit == Unit.Fraction

    override fun toString(): String = when (unit) {
        Unit.Cells -> "${value.toInt()}"
        Unit.Percent -> "${value.toInt()}%"
        Unit.Fraction -> "${value.toInt()}fr"
        Unit.Auto -> "auto"
    }

    public companion object {
        /** `Scalar(0, Cells)` constant. */
        public val ZERO: Scalar = Scalar(0.0, Unit.Cells)
        public val AUTO: Scalar = Scalar(0.0, Unit.Auto)

        /** Parse a token like `40`, `50%`, `1fr`, `auto`. Returns `null` on parse failure. */
        public fun parse(text: String): Scalar? {
            val s = text.trim()
            if (s.isEmpty()) return null
            if (s == "auto") return AUTO
            if (s.endsWith("fr")) {
                val v = s.removeSuffix("fr").toDoubleOrNull() ?: return null
                return Scalar(v, Unit.Fraction)
            }
            if (s.endsWith("%")) {
                val v = s.removeSuffix("%").toDoubleOrNull() ?: return null
                return Scalar(v, Unit.Percent)
            }
            val v = s.toDoubleOrNull() ?: return null
            return Scalar(v, Unit.Cells)
        }
    }
}

public enum class Unit { Cells, Percent, Fraction, Auto }
