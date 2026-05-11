package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Align
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.text.Justify
import tools.konsole.textual.widget.Widget

/**
 * Coloured rectangle for layout debugging. Mirrors Python textual's `Placeholder`.
 *
 * Renders as a bordered panel showing its [label] (defaults to the widget's
 * `id` or "placeholder"). Cycles through a palette of distinct colors based
 * on the placeholder's id so multiple placeholders in a layout are easy to
 * tell apart.
 *
 * Variants (matching textual): `Default`, `Size` (shows the widget's region
 * size — rendered on demand once compositor wiring lands), `Text` (custom).
 */
public open class Placeholder(
    public val label: String? = null,
    public val variant: Variant = Variant.Default,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public enum class Variant { Default, Size, Text }

    private val color: Color = colorFor(id ?: label ?: "placeholder")

    override fun render(): Renderable {
        val body = when (variant) {
            Variant.Default, Variant.Text -> label ?: id ?: "placeholder"
            Variant.Size -> "${id ?: "placeholder"}\n[dim](size unknown until compositor wires region)[/]"
        }
        return Panel(
            renderable = Align(
                Text(body, style = Style(color = Color.White, bold = true)),
                align = Justify.Center,
            ),
            box = Box.SQUARE,
            borderStyle = Style(color = color),
            style = Style(bgcolor = darken(color, 0.4)),
        )
    }

    private fun colorFor(seed: String): Color {
        val hash = seed.hashCode() and 0x7FFFFFFF
        val palette = listOf(
            Color.Rgb(0xe5, 0x5c, 0x5c),
            Color.Rgb(0xe9, 0x9d, 0x42),
            Color.Rgb(0xff, 0xc1, 0x07),
            Color.Rgb(0x2e, 0x86, 0x36),
            Color.Rgb(0x00, 0xa6, 0xa6),
            Color.Rgb(0x00, 0x4f, 0x9f),
            Color.Rgb(0x9f, 0x4f, 0xc8),
        )
        return palette[hash % palette.size]
    }

    private fun darken(color: Color, factor: Double): Color = when (color) {
        is Color.Rgb -> Color.Rgb(
            (color.r * factor).toInt().coerceIn(0, 255),
            (color.g * factor).toInt().coerceIn(0, 255),
            (color.b * factor).toInt().coerceIn(0, 255),
        )
        else -> color
    }
}
