package tools.konsole.textual.css

import tools.konsole.core.style.Color
import tools.konsole.rich.Style as RichStyle
import tools.konsole.rich.StyleParser
import tools.konsole.rich.geometry.Spacing

/**
 * Convert a list of [Declaration]s into a [Styles] instance.
 * Mirrors Python textual's `_styles_builder` — the dispatch table from
 * property name → builder function.
 *
 * Unknown property names are silently ignored (forward-compatible). Malformed
 * values fall back to "not set" (null) rather than throwing — TCSS errors are
 * collected separately by callers when reporting.
 */
public object StylesBuilder {

    public fun build(declarations: List<Declaration>): Styles {
        var s = Styles()
        for (d in declarations) s = s + applyOne(d)
        return s
    }

    private fun applyOne(d: Declaration): Styles {
        val v = d.value.trim()
        return when (d.name) {
            "display" -> Styles(display = parseDisplay(v))
            "visibility" -> Styles(visibility = parseVisibility(v))
            "layout" -> Styles(layout = parseLayout(v))

            "color" -> Styles(color = parseColor(v))
            "background" -> Styles(background = parseColor(v))
            "text-style" -> Styles(textStyle = StyleParser.parse(v))

            "width" -> Styles(width = Scalar.parse(v))
            "height" -> Styles(height = Scalar.parse(v))
            "min-width" -> Styles(minWidth = Scalar.parse(v))
            "min-height" -> Styles(minHeight = Scalar.parse(v))
            "max-width" -> Styles(maxWidth = Scalar.parse(v))
            "max-height" -> Styles(maxHeight = Scalar.parse(v))

            "padding" -> Styles(padding = parseSpacing(v))
            "margin" -> Styles(margin = parseSpacing(v))

            "border" -> Styles(border = parseBorder(v))
            "border-title" -> Styles(borderTitle = v.trim('"', '\''))

            "align" -> {
                val (h, v2) = parseAlignPair(v) ?: return Styles()
                Styles(align = h to v2, alignHorizontal = h, alignVertical = v2)
            }
            "align-horizontal" -> Styles(alignHorizontal = parseAlignH(v))
            "align-vertical" -> Styles(alignVertical = parseAlignV(v))

            "dock" -> Styles(dock = parseDock(v))
            "layer" -> Styles(layer = v)
            "layers" -> Styles(layers = v.split(Regex("\\s+")).filter { it.isNotEmpty() })

            "opacity" -> Styles(opacity = v.toDoubleOrNull())

            else -> Styles()
        }
    }

    private fun parseDisplay(v: String): Display? = when (v) {
        "block" -> Display.Block
        "none" -> Display.None
        else -> null
    }

    private fun parseVisibility(v: String): Visibility? = when (v) {
        "visible" -> Visibility.Visible
        "hidden" -> Visibility.Hidden
        else -> null
    }

    private fun parseLayout(v: String): LayoutKind? = when (v) {
        "vertical" -> LayoutKind.Vertical
        "horizontal" -> LayoutKind.Horizontal
        "grid" -> LayoutKind.Grid
        else -> null
    }

    private fun parseColor(v: String): Color? = Color.parse(v)

    private fun parseSpacing(v: String): Spacing? {
        val parts = v.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            1 -> Spacing.all(parts[0])
            2 -> Spacing.symmetric(horizontal = parts[1], vertical = parts[0])
            4 -> Spacing(top = parts[0], right = parts[1], bottom = parts[2], left = parts[3])
            else -> null
        }
    }

    private fun parseBorder(v: String): Pair<BorderStyle, Color>? {
        val tokens = v.trim().split(Regex("\\s+"))
        if (tokens.isEmpty()) return null
        val style = parseBorderStyle(tokens[0]) ?: return null
        val color = if (tokens.size > 1) Color.parse(tokens.drop(1).joinToString(" ")) ?: Color.Reset else Color.Reset
        return style to color
    }

    private fun parseBorderStyle(v: String): BorderStyle? = when (v) {
        "none" -> BorderStyle.None
        "solid" -> BorderStyle.Solid
        "round" -> BorderStyle.Round
        "heavy" -> BorderStyle.Heavy
        "double" -> BorderStyle.Double
        "ascii" -> BorderStyle.Ascii
        "dashed" -> BorderStyle.Dashed
        "hidden" -> BorderStyle.Hidden
        else -> null
    }

    private fun parseAlignPair(v: String): Pair<AlignHorizontal, AlignVertical>? {
        val parts = v.trim().split(Regex("\\s+"))
        if (parts.size != 2) return null
        val h = parseAlignH(parts[0]) ?: return null
        val ve = parseAlignV(parts[1]) ?: return null
        return h to ve
    }

    private fun parseAlignH(v: String): AlignHorizontal? = when (v) {
        "left" -> AlignHorizontal.Left
        "center" -> AlignHorizontal.Center
        "right" -> AlignHorizontal.Right
        else -> null
    }

    private fun parseAlignV(v: String): AlignVertical? = when (v) {
        "top" -> AlignVertical.Top
        "middle" -> AlignVertical.Middle
        "bottom" -> AlignVertical.Bottom
        else -> null
    }

    private fun parseDock(v: String): Dock? = when (v) {
        "top" -> Dock.Top
        "right" -> Dock.Right
        "bottom" -> Dock.Bottom
        "left" -> Dock.Left
        else -> null
    }
}
