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
import tools.konsole.textual.widgets.Horizontal
import tools.konsole.textual.widgets.Input
import tools.konsole.textual.widgets.Static
import tools.konsole.textual.widgets.Validator
import tools.konsole.textual.widgets.Vertical

/**
 * Modal text-prompt dialog. Mirrors the textual idiom
 * `app.push_screen(InputScreen("Name?")) { name -> … }`.
 *
 * The Input gets focus on mount; pressing Enter resolves with the typed
 * string; Escape resolves with `null`. An optional [validator] can reject
 * submissions — the dialog stays open until validation passes.
 */
public open class InputScreen(
    public val prompt: String,
    public val title: String = "Input",
    public val placeholder: String = "",
    public val initialValue: String = "",
    public val validator: Validator? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : ModalScreen<String>(id = id, classes = classes) {

    public val input: Input = Input(
        initial = initialValue,
        placeholder = placeholder,
        validator = validator,
        id = "input",
    )

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("escape", "dismiss", show = false),
        ),
    )

    init {
        input.start()
        input.onMessage<Input.Submitted> { evt ->
            if (evt.validationResult.isValid) dismiss(evt.value)
        }
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(
                Static(""),
                centeredDialog(),
                Static(""),
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
            promptColumn(),
            Static(""),
        ),
        widths = listOf(
            Scalar(1.0, LengthUnit.Fraction),
            Scalar(60.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Fraction),
        ),
    )

    private fun promptColumn(): Widget = Vertical(
        children = listOf(
            Static(panel()),
            input,
            Static("[dim]Enter=submit, Esc=cancel[/]"),
        ),
        heights = listOf(
            Scalar(3.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Cells),
        ),
    )

    private fun panel(): Panel = Panel(
        renderable = Text(prompt, style = Style(color = Color.White)),
        title = Text(title, style = Style(bold = true, color = Color.Cyan)),
        box = Box.ROUNDED,
        borderStyle = Style(color = Color.Cyan),
    )

    override fun start() {
        super.start()
        // Pull the App focus to the input on mount.
        var node: tools.konsole.textual.dom.DOMNode? = parent
        while (node != null && node !is tools.konsole.textual.app.App) node = node.parent
        if (node is tools.konsole.textual.app.App) node.setFocus(input)
    }
}
