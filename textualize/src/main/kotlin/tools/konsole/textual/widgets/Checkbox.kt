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
 * Boolean toggle rendered as `[X] Label` / `[ ] Label`.
 * Mirrors Python textual's `Checkbox`.
 *
 * Visually distinct from [Switch] — same semantics (bool reactive +
 * [Changed] message) but rendered as a labeled checkbox.
 */
public open class Checkbox(
    public val label: String = "",
    initial: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var value: Boolean = initial
        private set

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("enter", "toggle", "Toggle"),
            Binding("space", "toggle", "Toggle"),
        )
    )

    /** Flip [value] and post a [Changed] message. */
    public fun toggle() {
        value = !value
        post(Changed(this, value))
        refresh()
    }

    /** Binding-dispatched action: alias for [toggle]. */
    @Suppress("unused")
    public fun action_toggle() { toggle() }

    public fun set(newValue: Boolean) {
        if (newValue == value) return
        value = newValue
        post(Changed(this, value))
        refresh()
    }

    override fun render(): Renderable {
        val markStyle = Style(color = if (value) Color.Green else Color.DarkGrey, bold = true)
        val labelStyle = Style.NULL
        val text = Text()
        text.append("[", markStyle)
        text.append(if (value) "X" else " ", markStyle)
        text.append("] ", markStyle)
        text.append(label, labelStyle)
        return text
    }

    /** Posted when [value] changes. */
    public data class Changed(val checkbox: Checkbox, val value: Boolean) : Message()
}
