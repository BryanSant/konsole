package tools.konsole.textual.screen

import tools.konsole.core.style.Color
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.panel.Panel
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Horizontal
import tools.konsole.textual.widgets.Label
import tools.konsole.textual.widgets.Static
import tools.konsole.textual.widgets.Vertical

/**
 * Standard Yes/No confirmation modal. Mirrors textual's typical
 * `app.push_screen(ConfirmScreen("Save?")) { result -> … }` pattern.
 *
 * Resolves to:
 *  - `true`  when the user picks Yes (or presses `y`)
 *  - `false` when the user picks No (or presses `n`)
 *  - `null`  on Escape
 *
 * Usage:
 *
 * ```
 * val dialog = ConfirmScreen("Discard unsaved changes?", title = "Discard?")
 * dialog.onResult { answered ->
 *     if (answered == true) reallyDiscard()
 * }
 * app.pushScreen(dialog)
 * ```
 */
public open class ConfirmScreen(
    public val prompt: String,
    public val title: String = "Confirm",
    public val yesLabel: String = "Yes",
    public val noLabel: String = "No",
    id: String? = null,
    classes: Set<String> = emptySet(),
) : ModalScreen<Boolean>(id = id, classes = classes) {

    private val yesBtn = Button(yesLabel, variant = ButtonVariant.Primary, id = "yes")
    private val noBtn = Button(noLabel, variant = ButtonVariant.Default, id = "no")

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("escape", "dismiss", show = false),
            Binding("y", "yes", show = false),
            Binding("n", "no", show = false),
            Binding("enter", "yes", show = false),
        ),
    )

    @Suppress("unused") public fun action_yes() { dismiss(true) }
    @Suppress("unused") public fun action_no() { dismiss(false) }

    init {
        yesBtn.start()
        noBtn.start()
        yesBtn.onMessage<Button.Pressed> { dismiss(true) }
        noBtn.onMessage<Button.Pressed> { dismiss(false) }
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        // Center the dialog in the viewport via a Vertical with spacer 1fr
        // tracks on top and bottom.
        Vertical(
            children = listOf(
                Static(""),       // top spacer
                centeredDialog(),
                Static(""),       // bottom spacer
            ),
            heights = listOf(
                Scalar(1.0, LengthUnit.Fraction),
                Scalar(7.0, LengthUnit.Cells),
                Scalar(1.0, LengthUnit.Fraction),
            ),
        )
    )

    private fun centeredDialog(): Widget = Horizontal(
        children = listOf(
            Static(""),
            Static(panel()),
            Static(""),
        ),
        widths = listOf(
            Scalar(1.0, LengthUnit.Fraction),
            Scalar(60.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Fraction),
        ),
    )

    private fun panel(): Panel {
        val body = Text()
        body.append("$prompt\n\n", Style(color = Color.White))
        body.append("[y]es  [n]o   [bold](Enter=Yes, Esc=Cancel)[/]", Style(color = Color.DarkGrey, dim = true))
        return Panel(
            renderable = body,
            title = Text(title, style = Style(bold = true, color = Color.Cyan)),
            box = Box.ROUNDED,
            borderStyle = Style(color = Color.Cyan),
        )
    }
}
