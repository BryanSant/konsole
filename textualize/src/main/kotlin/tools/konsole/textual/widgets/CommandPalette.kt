package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.panel.Panel
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.message.Message
import tools.konsole.textual.screen.ModalScreen
import tools.konsole.textual.widget.Widget

/** One actionable item in a [CommandPalette]. */
public data class Command(
    public val id: String,
    public val title: String,
    public val description: String = "",
    public val keywords: List<String> = emptyList(),
)

/**
 * Modal fuzzy command launcher. Mirrors VSCode / Sublime / textual command
 * palettes. Push as a modal screen:
 *
 * ```
 * val palette = CommandPalette(listOf(
 *     Command("open", "Open File", "Open a file by path", listOf("file", "open")),
 *     Command("save", "Save", "Save current file"),
 *     Command("quit", "Quit", "Exit the application"),
 * ))
 * palette.onResult { picked -> picked?.let { runCommand(it.id) } }
 * app.pushScreen(palette)
 * ```
 *
 * Type to filter; Up/Down to move the selection; Enter to invoke; Esc to
 * cancel. The dialog resolves with the chosen [Command] or `null` if
 * dismissed.
 */
public open class CommandPalette(
    public val commands: List<Command>,
    public val title: String = "Commands",
    public val placeholder: String = "Type to search…",
    id: String? = null,
    classes: Set<String> = emptySet(),
) : ModalScreen<Command>(id = id, classes = classes) {

    public val input: Input = Input(placeholder = placeholder, id = "cp-input")
    public val list: OptionList = OptionList.ofLabels(
        labels = commands.map { it.title },
        id = "cp-list",
    )

    /** Filtered subset of [commands] reflecting the current query, in display order. */
    public var filtered: List<Command> = commands
        private set

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("escape", "dismiss", show = false),
            Binding("up", "prev", show = false),
            Binding("down", "next", show = false),
            Binding("enter", "invoke", show = false),
        ),
    )

    @Suppress("unused") public fun action_prev() { list.moveCursor(-1) }
    @Suppress("unused") public fun action_next() { list.moveCursor(1) }
    @Suppress("unused") public fun action_invoke() {
        val idx = list.highlightedIndex
        filtered.getOrNull(idx)?.let { dismiss(it) }
    }

    init {
        input.start()
        list.start()
        input.onMessage<Input.Changed> { applyFilter(it.value) }
        list.onMessage<OptionList.Selected> { evt ->
            filtered.getOrNull(evt.index)?.let { dismiss(it) }
        }
    }

    private fun applyFilter(query: String) {
        filtered = if (query.isBlank()) commands else commands.filter { it.matches(query) }
        list.setLabels(filtered.map { it.title })
        if (filtered.isNotEmpty()) list.highlight(0)
        post(Filtered(this, query, filtered))
    }

    private fun Command.matches(query: String): Boolean {
        val q = query.lowercase()
        if (q in title.lowercase()) return true
        if (q in description.lowercase()) return true
        return keywords.any { q in it.lowercase() }
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(
                Static(""),       // top spacer
                centeredDialog(),
                Static(""),       // bottom spacer
            ),
            heights = listOf(
                Scalar(1.0, LengthUnit.Fraction),
                Scalar(14.0, LengthUnit.Cells),
                Scalar(1.0, LengthUnit.Fraction),
            ),
        )
    )

    private fun centeredDialog(): Widget = Horizontal(
        children = listOf(
            Static(""),
            dialogColumn(),
            Static(""),
        ),
        widths = listOf(
            Scalar(1.0, LengthUnit.Fraction),
            Scalar(70.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Fraction),
        ),
    )

    private fun dialogColumn(): Widget = Vertical(
        children = listOf(
            Static(headerPanel()),
            input,
            list,
        ),
        heights = listOf(
            Scalar(3.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Fraction),
        ),
    )

    private fun headerPanel(): Panel = Panel(
        renderable = Text("Up/Down to move · Enter to run · Esc to cancel", style = Style(color = Color.DarkGrey, dim = true)),
        title = Text(title, style = Style(bold = true, color = Color.Cyan)),
        box = Box.ROUNDED,
        borderStyle = Style(color = Color.Cyan),
    )

    override fun start() {
        super.start()
        var node: tools.konsole.textual.dom.DOMNode? = parent
        while (node != null && node !is tools.konsole.textual.app.App) node = node.parent
        if (node is tools.konsole.textual.app.App) node.setFocus(input)
    }

    /** Posted whenever the filter is recomputed. */
    public data class Filtered(val palette: CommandPalette, val query: String, val results: List<Command>) : Message()
}
