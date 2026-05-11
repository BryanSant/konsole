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
 * Boolean toggle switch. Mirrors Python textual's `Switch`.
 *
 * Posts a [Changed] message whenever [value] flips. Toggle via mouse click,
 * Enter, or Space (matches textual's default bindings).
 *
 * @param value initial state.
 * @param animate whether to animate the toggle (cosmetic, no-op in Phase 9).
 */
public open class Switch(
    initial: Boolean = false,
    @Suppress("UNUSED_PARAMETER") public val animate: Boolean = true,
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

    /** Set [value] explicitly. Posts [Changed] only if the value differs. */
    public fun set(newValue: Boolean) {
        if (newValue == value) return
        value = newValue
        post(Changed(this, value))
        refresh()
    }

    override fun render(): Renderable {
        val onStyle = Style(color = Color.Green, bold = true)
        val offStyle = Style(color = Color.DarkGrey, bold = true)
        val text = Text()
        if (value) {
            text.append("[", offStyle); text.append("● ", onStyle); text.append("]", offStyle)
        } else {
            text.append("[", offStyle); text.append(" ●", offStyle); text.append("]", offStyle)
        }
        return text
    }

    /** Posted when [value] changes. Includes the new [value]. */
    public data class Changed(val switch: Switch, val value: Boolean) : Message()
}
