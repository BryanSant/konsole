package tools.konsole.rich

/**
 * Min/max horizontal cell width that a [Renderable] wants to occupy.
 *
 * Layout containers use this to budget width across children:
 *   - horizontal stack:  sum of mins, sum of maxes
 *   - vertical stack:    max of mins, max of maxes
 *   - clamp to a budget: [clamp]
 */
public data class Measurement(public val minimum: Int, public val maximum: Int) {

    init {
        require(minimum >= 0) { "minimum must be >= 0, got $minimum" }
        require(maximum >= minimum) { "maximum ($maximum) must be >= minimum ($minimum)" }
    }

    public val span: Int get() = maximum - minimum

    public fun clamp(min: Int = 0, max: Int = Int.MAX_VALUE): Measurement {
        val newMin = minimum.coerceIn(min, max)
        val newMax = maximum.coerceIn(newMin, max)
        return Measurement(newMin, newMax)
    }

    /** Compose horizontally (a row of cells). */
    public operator fun plus(other: Measurement): Measurement =
        Measurement(minimum + other.minimum, maximum + other.maximum)

    /** Compose vertically (a column of cells). */
    public fun stack(other: Measurement): Measurement =
        Measurement(maxOf(minimum, other.minimum), maxOf(maximum, other.maximum))

    public fun withMaximum(max: Int): Measurement =
        Measurement(minimum.coerceAtMost(max), max)

    public companion object {
        public val ZERO: Measurement = Measurement(0, 0)

        /** Look up or compute a measurement for a renderable. */
        public fun get(console: Console, options: RenderOptions, renderable: Renderable): Measurement {
            return if (renderable is Measurable) {
                renderable.measure(console, options).clamp(0, options.maxWidth)
            } else {
                defaultMeasure(console, options, renderable)
            }
        }

        /**
         * Default measurement: render once, find the longest unbreakable token (= min) and the
         * longest line (= max). Bounded by [RenderOptions.maxWidth].
         */
        public fun defaultMeasure(
            console: Console,
            options: RenderOptions,
            renderable: Renderable,
        ): Measurement {
            var longestLine = 0
            var longestToken = 0
            var lineLen = 0
            var tokenLen = 0
            for (segment in renderable.render(console, options)) {
                if (segment.control is Control.NewLine) {
                    if (lineLen > longestLine) longestLine = lineLen
                    if (tokenLen > longestToken) longestToken = tokenLen
                    lineLen = 0
                    tokenLen = 0
                    continue
                }
                for (ch in segment.text) {
                    if (ch == '\n') {
                        if (lineLen > longestLine) longestLine = lineLen
                        if (tokenLen > longestToken) longestToken = tokenLen
                        lineLen = 0
                        tokenLen = 0
                    } else {
                        lineLen += 1
                        if (ch.isWhitespace()) {
                            if (tokenLen > longestToken) longestToken = tokenLen
                            tokenLen = 0
                        } else {
                            tokenLen += 1
                        }
                    }
                }
            }
            if (lineLen > longestLine) longestLine = lineLen
            if (tokenLen > longestToken) longestToken = tokenLen
            val min = longestToken.coerceAtMost(options.maxWidth)
            val max = longestLine.coerceAtMost(options.maxWidth)
            return Measurement(min, maxOf(min, max))
        }
    }
}
