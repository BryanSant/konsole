package tools.konsole.rich.geometry

/**
 * Position offset in cell coordinates. Zero-based, with the origin at the
 * top-left of the parent container.
 */
public data class Offset(public val x: Int = 0, public val y: Int = 0) {
    public operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
    public operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
    public operator fun unaryMinus(): Offset = Offset(-x, -y)

    public companion object {
        public val ZERO: Offset = Offset(0, 0)
    }
}

/**
 * Inset spacing — top/right/bottom/left in cells (CSS-style ordering).
 */
public data class Spacing(
    public val top: Int = 0,
    public val right: Int = 0,
    public val bottom: Int = 0,
    public val left: Int = 0,
) {
    public val width: Int get() = left + right
    public val height: Int get() = top + bottom

    public operator fun plus(other: Spacing): Spacing = Spacing(
        top + other.top,
        right + other.right,
        bottom + other.bottom,
        left + other.left,
    )

    public companion object {
        public val ZERO: Spacing = Spacing(0, 0, 0, 0)

        /** Symmetric horizontal+vertical insets. */
        public fun symmetric(horizontal: Int = 0, vertical: Int = 0): Spacing =
            Spacing(top = vertical, right = horizontal, bottom = vertical, left = horizontal)

        /** Uniform inset on all sides. */
        public fun all(value: Int): Spacing = Spacing(value, value, value, value)
    }
}

/**
 * Axis-aligned rectangle in cell coordinates. `x`/`y` mark the top-left;
 * `width` and `height` are non-negative cell counts.
 */
public data class Region(
    public val x: Int,
    public val y: Int,
    public val width: Int,
    public val height: Int,
) {
    public val right: Int get() = x + width
    public val bottom: Int get() = y + height
    public val area: Int get() = width * height
    public val isEmpty: Boolean get() = width <= 0 || height <= 0

    /** Translate by an [Offset]. */
    public operator fun plus(o: Offset): Region = Region(x + o.x, y + o.y, width, height)

    /** Shrink by [insets] on each side, clamping at zero. */
    public fun shrink(insets: Spacing): Region = Region(
        x = x + insets.left,
        y = y + insets.top,
        width = (width - insets.width).coerceAtLeast(0),
        height = (height - insets.height).coerceAtLeast(0),
    )

    /** Test whether this region contains [point]. */
    public operator fun contains(point: Offset): Boolean =
        point.x in x until right && point.y in y until bottom

    /** Intersect with [other]; result may be empty. */
    public fun intersection(other: Region): Region {
        val x1 = maxOf(x, other.x)
        val y1 = maxOf(y, other.y)
        val x2 = minOf(right, other.right)
        val y2 = minOf(bottom, other.bottom)
        return Region(x1, y1, (x2 - x1).coerceAtLeast(0), (y2 - y1).coerceAtLeast(0))
    }

    public companion object {
        public val EMPTY: Region = Region(0, 0, 0, 0)
        public fun ofSize(width: Int, height: Int): Region = Region(0, 0, width, height)
    }
}
