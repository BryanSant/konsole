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
 * A selectable, keyboard-navigable list of options. Mirrors Python textual's
 * `OptionList`.
 *
 * Posts [Highlighted] when the cursor moves between options and [Selected]
 * when the user presses Enter / Space (or [selectCurrent] is called
 * programmatically). Options may be `String` (auto-wrapped) or
 * arbitrary `Renderable`s.
 *
 * @param options the items to choose from. Each pair is `(id, rendered)`.
 *   The `id` is what's reported by [Selected]; the rendered side can be
 *   markup or a [Renderable].
 */
public open class OptionList(
    public val options: List<Pair<String, Renderable>>,
    initialIndex: Int = 0,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public companion object {
        /**
         * String-only convenience factory — id and label both equal the
         * supplied [labels] entry (markup-parsed for display).
         */
        public fun ofLabels(
            labels: List<String>,
            initialIndex: Int = 0,
            id: String? = null,
            classes: Set<String> = emptySet(),
        ): OptionList = OptionList(labels.map { it to Markup.parse(it) }, initialIndex, id, classes)
    }

    public var highlightedIndex: Int = initialIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
        private set

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("up", "cursor_up", show = false),
            Binding("down", "cursor_down", show = false),
            Binding("home", "cursor_top", show = false),
            Binding("end", "cursor_bottom", show = false),
            Binding("enter", "select", "Select"),
            Binding("space", "select", "Select"),
        )
    )

    /** Move the highlight by [delta] options. Wraps at the ends. */
    public fun moveCursor(delta: Int) {
        if (options.isEmpty()) return
        val next = ((highlightedIndex + delta) % options.size + options.size) % options.size
        if (next == highlightedIndex) return
        highlightedIndex = next
        post(Highlighted(this, highlightedIndex, options[highlightedIndex].first))
        refresh()
    }

    /** Move the highlight to [index]. */
    public fun highlight(index: Int) {
        if (options.isEmpty()) return
        val clamped = index.coerceIn(0, options.size - 1)
        if (clamped == highlightedIndex) return
        highlightedIndex = clamped
        post(Highlighted(this, clamped, options[clamped].first))
        refresh()
    }

    /** Commit the currently highlighted option. */
    public fun selectCurrent(): Boolean {
        if (options.isEmpty()) return false
        return post(Selected(this, highlightedIndex, options[highlightedIndex].first))
    }

    override fun render(): Renderable {
        val text = Text()
        for ((i, option) in options.withIndex()) {
            val cursor = if (i == highlightedIndex) "▶ " else "  "
            val baseStyle = if (i == highlightedIndex) Style(color = Color.Black, bgcolor = Color.Cyan, bold = true)
                            else Style.NULL
            text.append(cursor, baseStyle)
            for (seg in option.second.render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 78))) {
                val merged = if (baseStyle.isNull) seg.style else baseStyle + (seg.style ?: Style.NULL)
                text.append(seg.text, merged)
            }
            if (i < options.lastIndex) text.append("\n")
        }
        return text
    }

    /** Posted when the highlighted option changes. */
    public data class Highlighted(val list: OptionList, val index: Int, val optionId: String) : Message()

    /** Posted when an option is selected (Enter/Space/[selectCurrent]). */
    public data class Selected(val list: OptionList, val index: Int, val optionId: String) : Message()
}

/**
 * Dropdown-style chooser — single-line collapsed display showing the
 * current selection; opens an [OptionList] when focused. Mirrors Python
 * textual's `Select`.
 *
 * Phase 9.5 ships the data model + render-when-closed. The "open dropdown"
 * UX requires the compositor's overlay system; until that lands (Phase 9.6),
 * [setSelected] is the primary API.
 *
 * @param options `(id, label)` pairs to choose from.
 * @param initialSelected the initial option id, or `null` for the prompt.
 * @param prompt label shown when nothing is selected.
 */
public open class Select<T>(
    public val options: List<Pair<T, String>>,
    initialSelected: T? = null,
    public val prompt: String = "Select an option",
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var selected: T? = initialSelected
        private set

    public val selectedLabel: String?
        get() = options.firstOrNull { it.first == selected }?.second

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("enter", "open", "Open"),
            Binding("space", "open", "Open"),
        )
    )

    /** Programmatically pick an option. */
    public fun setSelected(value: T?) {
        if (value == selected) return
        selected = value
        post(Changed(this, value))
        refresh()
    }

    override fun render(): Renderable {
        val text = Text()
        val style = if (hasFocus) Style(color = Color.White, bgcolor = Color.Rgb(0x2d, 0x32, 0x3f))
                    else Style(color = Color.White, bgcolor = Color.Rgb(0x1f, 0x22, 0x29))
        val label = selectedLabel ?: prompt
        text.append("⏷ ", Style(color = Color.Cyan, bold = true))
        text.append(label, if (selected == null) style + Style(dim = true, italic = true) else style)
        return text
    }

    /** Posted when [selected] changes. */
    public data class Changed<T>(val select: Select<T>, val value: T?) : Message()
}

/**
 * A list of arbitrary [Widget] children rendered vertically. Mirrors Python
 * textual's `ListView`. Unlike [OptionList], items are full widgets — useful
 * for cards / rich rows.
 */
public open class ListView(
    public val items: List<Widget>,
    initialIndex: Int = 0,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var highlightedIndex: Int = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        private set

    init { for (it in items) attach(it) }

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("up", "cursor_up", show = false),
            Binding("down", "cursor_down", show = false),
            Binding("enter", "select", "Select"),
        )
    )

    public fun moveCursor(delta: Int) {
        if (items.isEmpty()) return
        val next = ((highlightedIndex + delta) % items.size + items.size) % items.size
        if (next == highlightedIndex) return
        highlightedIndex = next
        post(Highlighted(this, highlightedIndex))
        refresh()
    }

    public fun selectCurrent(): Boolean = post(Selected(this, highlightedIndex))

    override fun render(): Renderable {
        val text = Text()
        for ((i, item) in items.withIndex()) {
            val cursor = if (i == highlightedIndex) "▶ " else "  "
            text.append(cursor, if (i == highlightedIndex) Style(color = Color.Cyan, bold = true) else Style.NULL)
            for (seg in item.render().render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 78))) {
                text.append(seg.text, seg.style)
            }
            if (i < items.lastIndex) text.append("\n")
        }
        return text
    }

    public data class Highlighted(val list: ListView, val index: Int) : Message()
    public data class Selected(val list: ListView, val index: Int) : Message()
}
