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
 * Single radio button — usually a child of a [RadioSet]. Renders as `(•) Label` / `( ) Label`.
 * Mirrors Python textual's `RadioButton`.
 */
public open class RadioButton(
    public val label: String = "",
    initial: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var value: Boolean = initial
        internal set

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("enter", "select", "Select"),
            Binding("space", "select", "Select"),
        )
    )

    override fun render(): Renderable {
        val markStyle = Style(color = if (value) Color.Green else Color.DarkGrey, bold = true)
        val text = Text()
        text.append("(", markStyle)
        text.append(if (value) "•" else " ", markStyle)
        text.append(") ", markStyle)
        text.append(label)
        return text
    }
}

/**
 * A group of mutually-exclusive [RadioButton]s. Mirrors Python textual's `RadioSet`.
 *
 * Posts [Changed] whenever the selected button changes; the previous button
 * (if any) is set to false automatically.
 *
 * Usage:
 * ```
 * val rs = RadioSet(
 *     RadioButton("Apple", initial = true),
 *     RadioButton("Banana"),
 *     RadioButton("Cherry"),
 * )
 * rs.select(2)  // pick Cherry
 * ```
 */
public open class RadioSet(
    public val buttons: List<RadioButton>,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public constructor(vararg buttons: RadioButton, id: String? = null, classes: Set<String> = emptySet()) :
        this(buttons.toList(), id, classes)

    init {
        // Enforce single-selection invariant on the initial state.
        val firstOn = buttons.indexOfFirst { it.value }
        if (firstOn >= 0) {
            for ((i, b) in buttons.withIndex()) if (i != firstOn) b.value = false
        }
        for (b in buttons) attach(b)
    }

    /** Index of the currently-selected button (`-1` if none). */
    public val pressedIndex: Int get() = buttons.indexOfFirst { it.value }

    /** The currently-selected button, or `null`. */
    public val pressed: RadioButton? get() = buttons.getOrNull(pressedIndex)

    /** Select the button at [index]. Idempotent if already selected. */
    public open fun select(index: Int) {
        if (index !in buttons.indices) return
        if (buttons[index].value) return
        for ((i, b) in buttons.withIndex()) b.value = (i == index)
        post(Changed(this, index, buttons[index]))
        refresh()
    }

    override fun render(): Renderable {
        // Stack vertically — when the layout engine wires its compositor (Phase 9.5)
        // children will lay themselves out; here we emit a vertical stack inline.
        val text = Text()
        for ((i, b) in buttons.withIndex()) {
            for (seg in b.render().render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 80))) {
                text.append(seg.text, seg.style)
            }
            if (i < buttons.lastIndex) text.append("\n")
        }
        return text
    }

    /** Posted when the selected button changes. */
    public data class Changed(val radioSet: RadioSet, val index: Int, val button: RadioButton) : Message()
}
