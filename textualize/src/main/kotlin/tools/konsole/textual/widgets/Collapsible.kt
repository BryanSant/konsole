package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/**
 * A disclosure widget — clicking the title bar expands/collapses the
 * [contents]. Mirrors Python textual's `Collapsible`.
 *
 * When collapsed, only the title row is shown (with `▶`).
 * When expanded, the title (`▼`) plus the contents are rendered below.
 *
 * @param title the header line.
 * @param collapsed initial state; `false` = expanded.
 * @param contents child widgets shown when expanded.
 */
public open class Collapsible(
    public val title: String = "",
    collapsed: Boolean = true,
    public val contents: List<Widget> = emptyList(),
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var collapsed: Boolean = collapsed
        private set

    init {
        for (c in contents) attach(c)
    }

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("enter", "toggle", "Toggle"),
            Binding("space", "toggle", "Toggle"),
        )
    )

    /** Toggle expand/collapse state. */
    public fun toggle() {
        collapsed = !collapsed
        post(Toggled(this, collapsed))
        refresh()
    }

    public fun expand() {
        if (!collapsed) return
        collapsed = false
        post(Toggled(this, false))
        refresh()
    }

    public fun collapse() {
        if (collapsed) return
        collapsed = true
        post(Toggled(this, true))
        refresh()
    }

    override fun render(): Renderable {
        val text = Text()
        val arrow = if (collapsed) "▶" else "▼"
        text.append("$arrow ", Style(color = Color.Cyan, bold = true))
        text.append(title, Style(bold = true))
        if (!collapsed) {
            text.append("\n")
            for ((i, child) in contents.withIndex()) {
                for (seg in child.render().render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 80))) {
                    text.append("  ")
                    text.append(seg.text, seg.style)
                }
                if (i < contents.lastIndex) text.append("\n")
            }
        }
        return text
    }

    /** Posted when [collapsed] changes. */
    public data class Toggled(val collapsible: Collapsible, val collapsed: Boolean) : Message()
}
