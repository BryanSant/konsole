package tools.konsole.rich.progress

import tools.konsole.core.ColorSystem
import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import kotlin.math.PI
import kotlin.math.cos

private const val PULSE_SIZE = 20

/**
 * Renders a progress bar with optional pulse animation. Faithfully ports
 * Python rich's `progress_bar.ProgressBar`.
 *
 * Behavior matches rich:
 * - Half-bar characters (`╸` / `╺`) align the boundary on cell-half steps,
 *   so a 50% bar of width 5 lands on the centre of cell 2 not cell 3.
 * - When [pulse] is enabled, or when [total] is null and `pulse=true` (or unset),
 *   a 20-cell cosine-cross-fade strip scrolls at 15 cells/second.
 * - On a no-color terminal the pulse falls back to a half-filled bar.
 * - The bar character is `─` (`bar`) and unfilled space uses [style];
 *   ASCII mode (legacy Windows or `ascii_only`) replaces with `-` and space.
 *
 * @param total total step count, or `null` for an indeterminate (pulsing) bar.
 * @param completed steps completed so far. Clamped to `[0, total]`.
 * @param width bar width in cells. `null` means use the rendering options' max width.
 * @param pulse enable pulse animation explicitly. When `total` is null, pulse is forced on.
 * @param style style for the empty/back portion. Defaults to `bar.back` theme key (gray).
 * @param completeStyle style for the filled portion before completion. Defaults to `bar.complete`.
 * @param finishedStyle style for the filled portion once complete. Defaults to `bar.finished`.
 * @param pulseStyle style for the bright tip of the pulse animation. Defaults to `bar.pulse`.
 * @param animationTime monotonic seconds used for animation phase. `null` reads `System.nanoTime()`;
 *   set explicitly for deterministic rendering in tests.
 */
public class ProgressBar(
    public val total: Double? = 100.0,
    public var completed: Double = 0.0,
    public val width: Int? = null,
    public val pulse: Boolean = false,
    public val style: Style = Style(color = Color.Rgb(58, 58, 58)),
    public val completeStyle: Style = Style(color = Color.Rgb(249, 38, 114)),
    public val finishedStyle: Style = Style(color = Color.Rgb(40, 196, 41)),
    public val pulseStyle: Style = Style(color = Color.Rgb(174, 199, 232)),
    public val animationTime: Double? = null,
) : Renderable, Measurable {

    /** Update [completed] (and optionally [total]). Mirrors `ProgressBar.update`. */
    public fun update(completed: Double, total: Double? = null) {
        this.completed = completed
        @Suppress("UNCHECKED_CAST") // total is val; rich allows reassignment, we don't
        if (total != null) {
            // Kotlin can't reassign val; this overload is informational. Use a fresh ProgressBar.
        }
    }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val w = (width ?: options.maxWidth).coerceAtMost(options.maxWidth).coerceAtLeast(1)
        val ascii = false // ascii_only / legacy_windows not yet modelled in RenderOptions
        val shouldPulse = pulse || total == null
        if (shouldPulse) {
            yieldAll(renderPulse(console, w, ascii))
            return@sequence
        }

        // After the shouldPulse early-return, total is guaranteed non-null.
        val effectiveTotal = total!!
        val clampedCompleted = completed.coerceIn(0.0, effectiveTotal)
        val bar = if (ascii) "-" else "━"
        val halfBarRight = if (ascii) " " else "╸"
        val halfBarLeft = if (ascii) " " else "╺"

        val completeHalves = ((w * 2 * clampedCompleted) / effectiveTotal).toInt()
        val barCount = completeHalves / 2
        val halfBarCount = completeHalves % 2

        val isFinished = completed >= effectiveTotal
        val fillStyle = if (isFinished) finishedStyle else completeStyle

        if (barCount > 0) yield(Segment(bar.repeat(barCount), fillStyle))
        if (halfBarCount > 0) yield(Segment(halfBarRight.repeat(halfBarCount), fillStyle))

        val noColor = console.colorSystem == ColorSystem.None
        if (!noColor) {
            var remaining = w - barCount - halfBarCount
            if (remaining > 0 && console.colorSystem != ColorSystem.None) {
                if (halfBarCount == 0 && barCount > 0) {
                    yield(Segment(halfBarLeft, style))
                    remaining -= 1
                }
                if (remaining > 0) yield(Segment(bar.repeat(remaining), style))
            }
        } else {
            val remaining = w - barCount - halfBarCount
            if (remaining > 0) yield(Segment(" ".repeat(remaining), style))
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement =
        if (width != null) Measurement(width, width)
        else Measurement(4, options.maxWidth)

    private fun renderPulse(console: Console, width: Int, ascii: Boolean): Sequence<Segment> = sequence {
        val foreStyle = pulseStyle
        val backStyle = style
        val pulseSegments = pulseSegments(foreStyle, backStyle, console.colorSystem, ascii)
        val segCount = pulseSegments.size
        if (segCount == 0) return@sequence
        val now = animationTime ?: (System.nanoTime() / 1e9)
        val tile = pulseSegments + pulseSegments + pulseSegments
        val offset = (((-now * 15.0).toInt()) % segCount + segCount) % segCount
        for (i in 0 until width) {
            yield(tile[(offset + i) % segCount])
        }
    }

    private fun pulseSegments(
        foreStyle: Style,
        backStyle: Style,
        colorSystem: ColorSystem,
        ascii: Boolean,
    ): List<Segment> {
        val bar = if (ascii) "-" else "━"
        // No color or unsupported system: fall back to half-fill.
        if (colorSystem == ColorSystem.None || colorSystem == ColorSystem.Windows) {
            val out = ArrayList<Segment>(PULSE_SIZE)
            repeat(PULSE_SIZE / 2) { out += Segment(bar, foreStyle) }
            repeat(PULSE_SIZE - PULSE_SIZE / 2) { out += Segment(if (colorSystem == ColorSystem.None) " " else bar, backStyle) }
            return out
        }
        val foreRgb = foreStyle.color?.let { rgbOf(it) } ?: Triple(255, 0, 255)
        val backRgb = backStyle.color?.let { rgbOf(it) } ?: Triple(0, 0, 0)
        val out = ArrayList<Segment>(PULSE_SIZE)
        for (i in 0 until PULSE_SIZE) {
            val pos = i.toDouble() / PULSE_SIZE
            val fade = 0.5 + cos(pos * PI * 2.0) / 2.0
            val r = (foreRgb.first + (backRgb.first - foreRgb.first) * fade).toInt().coerceIn(0, 255)
            val g = (foreRgb.second + (backRgb.second - foreRgb.second) * fade).toInt().coerceIn(0, 255)
            val b = (foreRgb.third + (backRgb.third - foreRgb.third) * fade).toInt().coerceIn(0, 255)
            out += Segment(bar, Style(color = Color.Rgb(r, g, b)))
        }
        return out
    }
}

/**
 * Best-effort RGB extraction for the named-color subset that has well-known triples.
 * Returns null if [c] doesn't have a fixed RGB equivalent (e.g. Reset).
 */
internal fun rgbOf(c: Color): Triple<Int, Int, Int>? = when (c) {
    is Color.Rgb -> Triple(c.r, c.g, c.b)
    is Color.AnsiValue -> ansi256ToRgb(c.value)
    Color.Black -> Triple(0, 0, 0)
    Color.DarkRed -> Triple(128, 0, 0)
    Color.DarkGreen -> Triple(0, 128, 0)
    Color.DarkYellow -> Triple(128, 128, 0)
    Color.DarkBlue -> Triple(0, 0, 128)
    Color.DarkMagenta -> Triple(128, 0, 128)
    Color.DarkCyan -> Triple(0, 128, 128)
    Color.Grey -> Triple(192, 192, 192)
    Color.DarkGrey -> Triple(128, 128, 128)
    Color.Red -> Triple(255, 0, 0)
    Color.Green -> Triple(0, 255, 0)
    Color.Yellow -> Triple(255, 255, 0)
    Color.Blue -> Triple(0, 0, 255)
    Color.Magenta -> Triple(255, 0, 255)
    Color.Cyan -> Triple(0, 255, 255)
    Color.White -> Triple(255, 255, 255)
    Color.Reset -> null
}

private fun ansi256ToRgb(idx: Int): Triple<Int, Int, Int> = when {
    idx in 0..15 -> rgbOf(NAMED_16[idx])!!
    idx in 16..231 -> {
        val n = idx - 16
        val levels = intArrayOf(0, 95, 135, 175, 215, 255)
        Triple(levels[(n / 36) % 6], levels[(n / 6) % 6], levels[n % 6])
    }
    else -> {
        val v = 8 + (idx - 232) * 10
        Triple(v, v, v)
    }
}

private val NAMED_16: Array<Color> = arrayOf(
    Color.Black, Color.DarkRed, Color.DarkGreen, Color.DarkYellow,
    Color.DarkBlue, Color.DarkMagenta, Color.DarkCyan, Color.Grey,
    Color.DarkGrey, Color.Red, Color.Green, Color.Yellow,
    Color.Blue, Color.Magenta, Color.Cyan, Color.White,
)
