package tools.konsole.textual.css

/**
 * A length value in a TCSS declaration — `40`, `50%`, `1fr`, `auto`.
 * Mirrors Python textual's `scalar.Scalar`.
 *
 * Resolution to concrete cell counts is delegated to layout passes which know
 * the parent's content dimension and total fractional units.
 */
public data class Scalar(public val value: Double, public val unit: LengthUnit) {

    /** Resolve to absolute cells given parent [containerSize] and (for fr units) the [totalFractions]. */
    public fun resolve(containerSize: Int, totalFractions: Double = 1.0): Int = when (unit) {
        LengthUnit.Cells -> value.toInt()
        LengthUnit.Percent -> ((value / 100.0) * containerSize).toInt()
        LengthUnit.Fraction -> if (totalFractions <= 0.0) 0 else ((value / totalFractions) * containerSize).toInt()
        LengthUnit.Auto -> -1 // sentinel — layout pass decides
    }

    public val isAuto: Boolean get() = unit == LengthUnit.Auto
    public val isFraction: Boolean get() = unit == LengthUnit.Fraction

    override fun toString(): String = when (unit) {
        LengthUnit.Cells -> "${value.toInt()}"
        LengthUnit.Percent -> "${value.toInt()}%"
        LengthUnit.Fraction -> "${value.toInt()}fr"
        LengthUnit.Auto -> "auto"
    }

    public companion object {
        /** `Scalar(0, Cells)` constant. */
        public val ZERO: Scalar = Scalar(0.0, LengthUnit.Cells)
        public val AUTO: Scalar = Scalar(0.0, LengthUnit.Auto)

        /** Parse a token like `40`, `50%`, `1fr`, `auto`. Returns `null` on parse failure. */
        public fun parse(text: String): Scalar? {
            val s = text.trim()
            if (s.isEmpty()) return null
            if (s == "auto") return AUTO
            if (s.endsWith("fr")) {
                val v = s.removeSuffix("fr").toDoubleOrNull() ?: return null
                return Scalar(v, LengthUnit.Fraction)
            }
            if (s.endsWith("%")) {
                val v = s.removeSuffix("%").toDoubleOrNull() ?: return null
                return Scalar(v, LengthUnit.Percent)
            }
            val v = s.toDoubleOrNull() ?: return null
            return Scalar(v, LengthUnit.Cells)
        }
    }
}

public enum class LengthUnit { Cells, Percent, Fraction, Auto }
