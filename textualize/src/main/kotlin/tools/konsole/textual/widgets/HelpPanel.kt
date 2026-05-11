package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.panel.Panel
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.widget.Widget

/**
 * Floating help panel showing the active key [bindings]. Mirrors Python
 * textual's `HelpPanel`. Renders the supplied bindings as `key  description`
 * rows inside a bordered Panel.
 *
 * Typically pinned to [tools.konsole.textual.compositor.Compositor.POPUP] so
 * it floats over the app content when the user presses `?` or similar.
 */
public open class HelpPanel(
    public val title: String = "Key bindings",
    public val helpBindings: BindingsMap = BindingsMap(),
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override val preferredLayer: tools.konsole.textual.compositor.Compositor.Layer
        get() = tools.konsole.textual.compositor.Compositor.POPUP

    override fun render(): Renderable {
        val body = Text()
        for ((i, b) in helpBindings.all().withIndex()) {
            if (!b.show) continue
            body.append(b.key.padEnd(12), Style(color = Color.Yellow, bold = true))
            body.append(b.description.ifEmpty { b.action })
            if (i < helpBindings.all().lastIndex) body.append("\n")
        }
        return Panel(
            renderable = body,
            title = Text(title, style = Style(bold = true)),
            box = Box.ROUNDED,
            borderStyle = Style(color = Color.Cyan),
        )
    }
}

/**
 * Single-binding key chip — small inline hint, the building block textual's
 * `Footer` uses. Mirrors Python textual's `KeyPanel.Key`.
 *
 * Renders as `[ key ] description`, suitable for placing in a horizontal
 * row of bindings (Footer does this internally already; KeyPanel is the
 * exposed unit for custom chrome).
 */
public open class KeyPanel(
    public val binding: Binding,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override fun render(): Renderable {
        val text = Text()
        text.append(" ${binding.key} ", Style(color = Color.Yellow, bgcolor = Color.Rgb(0x00, 0x4f, 0x9f), bold = true))
        text.append(" ${binding.description.ifEmpty { binding.action }} ", Style(color = Color.White))
        return text
    }
}
