package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup
import tools.konsole.textual.widget.Scrollable
import tools.konsole.textual.widget.Widget

/**
 * Scrolling log buffer — append lines or renderables, see them stacked.
 * Mirrors Python textual's `RichLog` widget.
 *
 * Older lines drop off when [maxLines] is exceeded. `auto_scroll` (always-on
 * for now — Phase 9.5 will expose it) keeps the bottom of the buffer visible.
 *
 * @param maxLines bounded buffer size; older lines are dropped FIFO.
 *   `null` for unbounded.
 * @param highlight enable rich's default repr highlighter on plain string writes.
 *   Mirrors textual's `highlight` flag.
 * @param markup parse markup in string writes.
 */
public open class RichLog(
    public val maxLines: Int? = 1000,
    public val highlight: Boolean = false,
    public val markup: Boolean = true,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Scrollable {

    private val lines: ArrayDeque<Renderable> = ArrayDeque()

    override var scrollX: Int = 0
    override var scrollY: Int = 0
    override val contentWidth: Int get() = 0  // computed at render time
    override val contentHeight: Int get() = lines.size

    /** Append a string (markup-parsed if [markup]). */
    public open fun write(text: String) {
        val rendered = if (markup) Markup.parse(text) else Text(text)
        write(rendered)
    }

    /** Append an arbitrary [Renderable]. */
    public open fun write(renderable: Renderable) {
        lines.addLast(renderable)
        if (maxLines != null && lines.size > maxLines) lines.removeFirst()
        // Auto-scroll to bottom
        scrollY = (lines.size - 1).coerceAtLeast(0)
        refresh()
    }

    /** Drop every buffered line. */
    public open fun clear() {
        if (lines.isEmpty()) return
        lines.clear()
        scrollY = 0
        refresh()
    }

    /** Number of currently-buffered lines. */
    public val size: Int get() = lines.size

    override fun renderStrips(width: Int, startY: Int, count: Int): List<tools.konsole.rich.Strip> {
        if (count <= 0) return emptyList()
        val result = ArrayList<tools.konsole.rich.Strip>(count)
        for (i in 0 until count) {
            val line = lines.getOrNull(startY + i)
            if (line == null) { result += tools.konsole.rich.Strip.EMPTY; continue }
            val segments = mutableListOf<tools.konsole.rich.Segment>()
            for (seg in line.render(tools.konsole.rich.Console.string(width = width), tools.konsole.rich.RenderOptions(maxWidth = width))) {
                if (seg.text == "\n") continue  // single-line entries only
                segments += seg
            }
            result += tools.konsole.rich.Strip.of(segments).adjustCellLength(width)
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
    @Suppress("unused") public fun action_scroll_end() { scrollEnd(); refresh() }

    override fun render(): Renderable {
        // Concatenate all lines with newlines between them; the compositor handles
        // viewport clipping once the full scroll-view pipeline lands (Phase 9.6).
        val combined = Text()
        for ((i, line) in lines.withIndex()) {
            for (seg in line.render(tools.konsole.rich.Console.string(width = 120), tools.konsole.rich.RenderOptions(maxWidth = 120))) {
                combined.append(seg.text, seg.style)
            }
            if (i < lines.size - 1) combined.append("\n")
        }
        return combined
    }
}
