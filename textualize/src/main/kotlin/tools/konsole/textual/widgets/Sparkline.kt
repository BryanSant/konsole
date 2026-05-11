package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.widget.Widget

/**
 * A tiny inline chart drawn with eight Unicode block heights. Mirrors
 * Python textual's `Sparkline`.
 *
 * Each data point becomes one cell wide, with height chosen from the eight
 * lower-block glyphs `▁▂▃▄▅▆▇█`. The series is auto-scaled so the largest
 * value reaches the tallest glyph; supply [maxValue] to fix the scale (e.g.
 * for percentages set `maxValue = 100`).
 *
 * @param data points to plot.
 * @param summary how to reduce when more points than [width] cells are
 *   supplied. `Mean` averages the bucket; `Max` takes the highest.
 * @param style style applied to every glyph.
 */
public open class Sparkline(
    data: List<Double> = emptyList(),
    public val maxValue: Double? = null,
    public val summary: SummaryFunction = SummaryFunction.Mean,
    public val style: Style = Style(color = Color.Cyan),
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var data: List<Double> = data
        private set

    public enum class SummaryFunction { Mean, Max, Min }

    /** Replace the data series. */
    public fun update(data: List<Double>) {
        this.data = data
        refresh()
    }

    override fun render(): Renderable {
        if (data.isEmpty()) return Text("")
        val maxV = maxValue ?: data.maxOrNull() ?: 1.0
        val effectiveMax = if (maxV == 0.0) 1.0 else maxV
        val text = Text()
        for (value in data) {
            val normalised = (value.coerceIn(0.0, effectiveMax)) / effectiveMax
            val idx = (normalised * (BLOCKS.size - 1)).toInt().coerceIn(0, BLOCKS.size - 1)
            text.append(BLOCKS[idx].toString(), style)
        }
        return text
    }

    public companion object {
        /** The 8-step lower-block glyph ladder. Index 0 = blank, 7 = full. */
        public val BLOCKS: List<Char> = listOf(' ', '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█')
    }
}
