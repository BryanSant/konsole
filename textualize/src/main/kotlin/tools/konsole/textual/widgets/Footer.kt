package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.widget.Widget

/**
 * Application chrome footer — displays the active key bindings.
 * Mirrors Python textual's `Footer` (visual layout only; the dynamic update
 * on screen-stack change happens once the compositor wires its `Mount` event).
 *
 * @param bindings the bindings to display. Typically `app.bindings` or `screen.bindings`.
 */
public open class Footer(
    override val bindings: BindingsMap = BindingsMap(),
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    private val baseStyle = Style(bgcolor = tools.konsole.core.style.Color.Blue, color = tools.konsole.core.style.Color.White)
    private val keyStyle = Style(bgcolor = tools.konsole.core.style.Color.Blue, color = tools.konsole.core.style.Color.Yellow, bold = true)

    override fun render(): Renderable {
        val text = Text()
        for ((idx, binding) in bindings.all().withIndex()) {
            if (!binding.show) continue
            if (idx > 0) text.append("  ", baseStyle)
            text.append(" ${binding.key} ", keyStyle)
            text.append(" ${binding.description.ifEmpty { binding.action }} ", baseStyle)
        }
        return text
    }
}
