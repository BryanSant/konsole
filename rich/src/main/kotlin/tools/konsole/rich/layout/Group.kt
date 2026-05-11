package tools.konsole.rich.layout

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment

/**
 * Renders multiple [Renderable]s vertically as a single unit. Mirrors `rich.console.Group`.
 *
 * Each child's output is separated by a [Segment.LINE]. No outer formatting is applied.
 */
public class Group(public val children: List<Renderable>) : Measurable {

    public constructor(vararg children: Renderable) : this(children.toList())

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        for ((i, child) in children.withIndex()) {
            if (i > 0) yield(Segment.LINE)
            yieldAll(child.render(console, options))
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        var min = 0
        var max = 0
        for (child in children) {
            val m = Measurement.get(console, options, child)
            min = maxOf(min, m.minimum)
            max = maxOf(max, m.maximum)
        }
        return Measurement(min, max)
    }
}

/** Convenience function for ad-hoc grouping. */
public fun group(vararg renderables: Renderable): Group = Group(renderables.toList())
