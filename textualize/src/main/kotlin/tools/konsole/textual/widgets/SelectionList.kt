package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/**
 * Multi-select list. Mirrors Python textual's `SelectionList`.
 *
 * Unlike [OptionList] (single highlight + selection), each row has its own
 * checkbox state — Space/Enter toggles the highlighted row, arrow keys
 * navigate. Posts [SelectionChanged] for individual flips and [Selected]
 * when the user confirms the whole selection (e.g. via a hotkey).
 *
 * @param options `(id, label, initiallySelected)` triples.
 */
public open class SelectionList(
    public val options: List<Triple<String, String, Boolean>>,
    initialIndex: Int = 0,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    private val _selected: MutableSet<String> = options.filter { it.third }.map { it.first }.toMutableSet()

    public var highlightedIndex: Int = initialIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
        private set

    public val selectedIds: Set<String> get() = _selected.toSet()

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("up", "cursor_up", show = false),
            Binding("down", "cursor_down", show = false),
            Binding("space", "toggle", "Toggle"),
            Binding("enter", "toggle", "Toggle"),
            Binding("ctrl+a", "select_all", "Select all"),
            Binding("ctrl+d", "deselect_all", "Deselect all"),
        )
    )

    /** Toggle membership of the highlighted option. */
    public fun toggleCurrent() {
        val opt = options.getOrNull(highlightedIndex) ?: return
        val id = opt.first
        val nowSelected = id !in _selected
        if (nowSelected) _selected += id else _selected -= id
        post(SelectionChanged(this, id, nowSelected, _selected.toSet()))
        refresh()
    }

    /** Toggle a specific option by id. */
    public fun toggle(id: String) {
        if (options.none { it.first == id }) return
        val nowSelected = id !in _selected
        if (nowSelected) _selected += id else _selected -= id
        post(SelectionChanged(this, id, nowSelected, _selected.toSet()))
        refresh()
    }

    public fun selectAll() {
        _selected.clear()
        _selected += options.map { it.first }
        post(SelectionChanged(this, null, null, _selected.toSet()))
        refresh()
    }

    public fun deselectAll() {
        _selected.clear()
        post(SelectionChanged(this, null, null, _selected.toSet()))
        refresh()
    }

    public fun moveCursor(delta: Int) {
        if (options.isEmpty()) return
        val next = ((highlightedIndex + delta) % options.size + options.size) % options.size
        if (next == highlightedIndex) return
        highlightedIndex = next
        refresh()
    }

    // Binding-dispatched actions
    @Suppress("unused") public fun action_cursor_up() { moveCursor(-1) }
    @Suppress("unused") public fun action_cursor_down() { moveCursor(1) }
    @Suppress("unused") public fun action_toggle() { toggleCurrent() }
    @Suppress("unused") public fun action_select_all() { selectAll() }
    @Suppress("unused") public fun action_deselect_all() { deselectAll() }

    override fun render(): Renderable {
        val text = Text()
        for ((i, opt) in options.withIndex()) {
            val (id, label, _) = opt
            val checkedMark = if (id in _selected) "[x]" else "[ ]"
            val cursor = if (i == highlightedIndex) "▶ " else "  "
            val rowStyle = if (i == highlightedIndex) Style(color = Color.Black, bgcolor = Color.Cyan, bold = true)
                           else Style.NULL
            text.append(cursor, rowStyle)
            text.append("$checkedMark ", rowStyle + Style(color = if (id in _selected) Color.Green else Color.DarkGrey, bold = true))
            for (seg in Markup.parse(label).render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 80))) {
                text.append(seg.text, rowStyle + (seg.style ?: Style.NULL))
            }
            if (i < options.lastIndex) text.append("\n")
        }
        return text
    }

    /** Posted on every toggle. `id`/`isSelected` are null when the event is a bulk select_all/deselect_all. */
    public data class SelectionChanged(
        val list: SelectionList,
        val id: String?,
        val isSelected: Boolean?,
        val currentSelection: Set<String>,
    ) : Message()
}
