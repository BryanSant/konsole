package tools.konsole.rich.spinner

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup

/**
 * An animated spinner. Each frame is one cell of [frames]; rendering picks the frame for
 * the current monotonic time. Combine with optional [text] and [style].
 *
 * Spinner is a [Renderable] — its frame is determined by `System.nanoTime()` at render time
 * (so it animates naturally inside a [tools.konsole.rich.live.Live]).
 *
 * Mirrors `rich.spinner.Spinner`.
 */
public class Spinner(
    public val frames: List<String>,
    public val interval: Long = 80L, // milliseconds per frame
    public val text: Renderable? = null,
    public val style: Style? = null,
    public val speed: Double = 1.0,
) : Measurable {

    public constructor(name: String, text: Renderable? = null, style: Style? = null, speed: Double = 1.0) :
        this(SpinnerRegistry.frames(name), SpinnerRegistry.interval(name), text, style, speed)

    /** The current frame at the given time, defaulting to monotonic clock. */
    public fun frame(now: Long = System.currentTimeMillis()): String {
        val advance = (now * speed).toLong()
        val idx = ((advance / interval) % frames.size).toInt()
        return frames[idx.coerceAtLeast(0)]
    }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = buildList {
        val frame = frame()
        if (style != null) add(Segment(frame, style)) else add(Segment(frame))
        if (text != null) {
            add(Segment(" "))
            for (s in text.render(console, options.withMaxWidth((options.maxWidth - frame.length - 1).coerceAtLeast(1)))) {
                add(s)
            }
        }
    }.asSequence()

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val frameWidth = frames.maxOf { it.length }
        val textWidth = text?.let { Measurement.get(console, options, it).maximum + 1 } ?: 0
        val total = (frameWidth + textWidth).coerceAtMost(options.maxWidth)
        return Measurement(total, total)
    }

    public companion object {
        public fun of(name: String, text: String? = null, style: Style? = null): Spinner =
            Spinner(name, text?.let { Markup.parse(it) }, style)
    }
}

/** Static spinner data — full catalogue of Python rich's spinners (73 entries, see [SPINNERS]). */
internal object SpinnerRegistry {

    fun frames(name: String): List<String> = entry(name).frames
    fun interval(name: String): Long = entry(name).interval.toLong()

    val names: Set<String> get() = SPINNERS.keys

    private fun entry(name: String): SpinnerData =
        SPINNERS[name] ?: error("Unknown spinner: '$name'. Known: ${SPINNERS.keys.sorted()}")
}

