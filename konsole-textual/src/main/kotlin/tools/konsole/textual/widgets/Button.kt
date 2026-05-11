package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.layout.Align
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.text.Justify
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/** Button visual variants. Mirrors textual's `ButtonVariant`. */
public enum class ButtonVariant { Default, Primary, Success, Warning, Error }

/**
 * Clickable button. Mirrors Python textual's `Button`.
 *
 * Posts a [Pressed] message on press (via mouse click, Enter, or Space).
 * Subclass [Widget]'s message pump or use `app.onMessage<Button.Pressed> { … }`
 * to handle presses.
 *
 * @param label button text (supports markup).
 * @param variant visual style.
 * @param disabled greyed-out and ignores input.
 */
public open class Button(
    public val label: String = "",
    public val variant: ButtonVariant = ButtonVariant.Default,
    public val disabled: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override val canFocus: Boolean get() = !disabled

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("enter", "press", "Press"),
            Binding("space", "press", "Press"),
        )
    )

    /** Programmatically press the button — posts a [Pressed] message. */
    public fun press(): Boolean = post(Pressed(this))

    override fun render(): Renderable {
        val text = Markup.parse(label)
        return Align(text, align = Justify.Center, style = styleFor(variant, disabled))
    }

    private fun styleFor(variant: ButtonVariant, disabled: Boolean): Style {
        val base = when (variant) {
            ButtonVariant.Default -> Style(color = Color.White, bgcolor = Color.Rgb(0x3a, 0x3d, 0x43), bold = true)
            ButtonVariant.Primary -> Style(color = Color.White, bgcolor = Color.Rgb(0x00, 0x4f, 0x9f), bold = true)
            ButtonVariant.Success -> Style(color = Color.White, bgcolor = Color.Rgb(0x2e, 0x86, 0x36), bold = true)
            ButtonVariant.Warning -> Style(color = Color.Black, bgcolor = Color.Rgb(0xe9, 0x9d, 0x42), bold = true)
            ButtonVariant.Error -> Style(color = Color.White, bgcolor = Color.Rgb(0xe5, 0x5c, 0x5c), bold = true)
        }
        return if (disabled) base.copy(dim = true) else base
    }

    /** Posted when the button is pressed. Carries a reference to the originating [button]. */
    public data class Pressed(val button: Button) : Message()
}
