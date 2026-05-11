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
        scrollY = (lines.size - 1).coerceAtLeast(0)
        refresh()
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

    override fun render(): Renderable = Text(lines.joinToString("\n"))
}
