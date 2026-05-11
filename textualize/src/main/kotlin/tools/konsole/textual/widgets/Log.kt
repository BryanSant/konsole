package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Text
import tools.konsole.textual.widget.Scrollable
import tools.konsole.textual.widget.Widget

/**
 * Plain-text scrolling log buffer. Mirrors Python textual's `Log` widget —
 * the simpler cousin of [RichLog]: no markup parsing, no rich renderables,
 * just raw lines of monospace text.
 *
 * Use when:
 *  - log content is already styled by some other source (e.g. command output)
 *  - you want predictable byte-for-byte fidelity (no markup surprises)
 *  - performance matters (no Markup parser per write)
 *
 * For markup-aware or `Renderable`-carrying logs, use [RichLog] instead.
 *
 * @param maxLines bounded buffer; older lines are dropped FIFO. `null` = unbounded.
 */
public open class Log(
    public val maxLines: Int? = 1000,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Scrollable {

    private val lines: ArrayDeque<String> = ArrayDeque()

    override var scrollX: Int = 0
    override var scrollY: Int = 0
    override val contentWidth: Int get() = lines.maxOfOrNull { it.length } ?: 0
    override val contentHeight: Int get() = lines.size

    /** Append a single line (newlines split into multiple entries). */
    public open fun write(text: String) {
        for (line in text.split('\n')) {
            lines.addLast(line)
            if (maxLines != null && lines.size > maxLines) lines.removeFirst()
        }
        autoScrollToBottom()
        refresh()
    }

    // scrollY is the index of the *first visible* line, so the auto-scroll
    // position is contentHeight - viewportHeight (not contentHeight - 1).
    // The viewport height lives on `lastRegion`; until the first render
    // there is no region, so leave scrollY untouched in that case.
    private fun autoScrollToBottom() {
        val viewportH = lastRegion?.height ?: return
        scrollY = (lines.size - viewportH).coerceAtLeast(0)
    }

    /** Append [text] without a trailing newline. */
    public open fun writeLine(text: String): Unit = write(text)

    /** Drop every buffered line. */
    public open fun clear() {
        if (lines.isEmpty()) return
        lines.clear()
        scrollY = 0
        refresh()
    }

    public val size: Int get() = lines.size

    override fun renderStrips(width: Int, startY: Int, count: Int): List<tools.konsole.rich.Strip> {
        if (count <= 0) return emptyList()
        val result = ArrayList<tools.konsole.rich.Strip>(count)
        for (i in 0 until count) {
            val line = lines.getOrNull(startY + i)
            result += if (line != null) tools.konsole.rich.Strip.of(tools.konsole.rich.Segment(line)).adjustCellLength(width)
                      else tools.konsole.rich.Strip.EMPTY
        }
        return result
    }

    override val canFocus: Boolean get() = true

    override val bindings: tools.konsole.textual.binding.BindingsMap = tools.konsole.textual.binding.BindingsMap(
        listOf(
            tools.konsole.textual.binding.Binding("up", "scroll_up", show = false),
            tools.konsole.textual.binding.Binding("down", "scroll_down", show = false),
            tools.konsole.textual.binding.Binding("left", "scroll_left", show = false),
            tools.konsole.textual.binding.Binding("right", "scroll_right", show = false),
            tools.konsole.textual.binding.Binding("pageup", "scroll_page_up", show = false),
            tools.konsole.textual.binding.Binding("pagedown", "scroll_page_down", show = false),
            tools.konsole.textual.binding.Binding("home", "scroll_home", show = false),
            tools.konsole.textual.binding.Binding("end", "scroll_end", show = false),
        )
    )

    @Suppress("unused") public fun action_scroll_up() { scrollBy(dy = -1); refresh() }
    @Suppress("unused") public fun action_scroll_down() { scrollBy(dy = 1); refresh() }
    @Suppress("unused") public fun action_scroll_left() { scrollBy(dx = -2); refresh() }
    @Suppress("unused") public fun action_scroll_right() { scrollBy(dx = 2); refresh() }
    @Suppress("unused") public fun action_scroll_page_up() { scrollPageUp(lastRegion?.height ?: 10); refresh() }
    @Suppress("unused") public fun action_scroll_page_down() { scrollPageDown(lastRegion?.height ?: 10); refresh() }
    @Suppress("unused") public fun action_scroll_home() { scrollHome(); refresh() }
    @Suppress("unused") public fun action_scroll_end() { autoScrollToBottom(); refresh() }

    override fun render(): Renderable = Text(lines.joinToString("\n"))
}
