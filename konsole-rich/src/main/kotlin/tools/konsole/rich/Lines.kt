package tools.konsole.rich

/**
 * A list of [Text] lines rendered vertically. Mirrors rich's `containers.Lines`.
 *
 * Each entry becomes one line in the output stream. Useful when a renderable
 * has already done its own line splitting and you want to forward the lines
 * verbatim without further wrapping.
 */
public data class Lines(public val lines: List<Text>) : Renderable, Measurable {

    public constructor(vararg lines: Text) : this(lines.toList())

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val newline = Segment("\n")
        for ((i, line) in lines.withIndex()) {
            for (s in line.render(console, options)) yield(s)
            if (i < lines.size - 1) yield(newline)
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        if (lines.isEmpty()) return Measurement(0, 0)
        var min = 0
        var max = 0
        for (line in lines) {
            val m = line.measure(console, options)
            if (m.minimum > min) min = m.minimum
            if (m.maximum > max) max = m.maximum
        }
        return Measurement(min, max)
    }
}
