package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.markdown.Markdown as RichMarkdown
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.syntax.SyntaxTheme
import tools.konsole.textual.widget.Widget

/**
 * Markdown reader with a side-mounted table of contents. Mirrors Python
 * textual's `MarkdownViewer` — pairs a [Markdown] body widget with a TOC
 * sidebar built from `#` / `##` headings extracted from the source.
 *
 * Phase 9.11 ships the visual layout — the TOC is computed up-front when
 * [update] is called. Interactive TOC navigation (click a heading to scroll
 * the body to that section) lands in Phase 9.12 alongside the full
 * scroll-viewport pipeline.
 *
 * @param markdown initial Markdown source.
 * @param showTableOfContents render the TOC sidebar at all.
 * @param codeTheme syntax theme for fenced code blocks.
 */
public open class MarkdownViewer(
    markdown: String = "",
    public val showTableOfContents: Boolean = true,
    public val codeTheme: SyntaxTheme = SyntaxTheme.MONOKAI,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), tools.konsole.textual.widget.Scrollable {

    override var scrollX: Int = 0
    override var scrollY: Int = 0
    override val contentWidth: Int get() = 80
    override val contentHeight: Int get() = markdown.lines().size

    public var markdown: String = markdown
        private set

    public var tableOfContents: List<TocEntry> = extractToc(markdown)
        private set

    /** Replace the Markdown source and recompute the TOC. */
    public fun update(markdown: String) {
        this.markdown = markdown
        this.tableOfContents = extractToc(markdown)
        refresh()
    }

    override val canFocus: Boolean get() = true

    override val bindings: tools.konsole.textual.binding.BindingsMap = tools.konsole.textual.binding.BindingsMap(
        listOf(
            tools.konsole.textual.binding.Binding("up", "scroll_up", show = false),
            tools.konsole.textual.binding.Binding("down", "scroll_down", show = false),
            tools.konsole.textual.binding.Binding("pageup", "scroll_page_up", show = false),
            tools.konsole.textual.binding.Binding("pagedown", "scroll_page_down", show = false),
            tools.konsole.textual.binding.Binding("home", "scroll_home", show = false),
            tools.konsole.textual.binding.Binding("end", "scroll_end", show = false),
        )
    )

    @Suppress("unused") public fun action_scroll_up() { scrollBy(dy = -1); refresh() }
    @Suppress("unused") public fun action_scroll_down() { scrollBy(dy = 1); refresh() }
    @Suppress("unused") public fun action_scroll_page_up() { scrollPageUp(lastRegion?.height ?: 20); refresh() }
    @Suppress("unused") public fun action_scroll_page_down() { scrollPageDown(lastRegion?.height ?: 20); refresh() }
    @Suppress("unused") public fun action_scroll_home() { scrollHome(); refresh() }
    @Suppress("unused") public fun action_scroll_end() { scrollEnd(); refresh() }

    override fun render(): Renderable {
        val text = Text()
        if (showTableOfContents && tableOfContents.isNotEmpty()) {
            // Render TOC inline above the body. Two-column layout with proper
            // side-by-side rendering lands in Phase 9.12 when the compositor
            // exposes per-widget regions to widgets.
            val toc = Text()
            for (entry in tableOfContents) {
                val indent = "  ".repeat(entry.level - 1)
                toc.append(indent, Style(color = Color.DarkGrey))
                toc.append(entry.title, Style(color = Color.Cyan, bold = entry.level == 1))
                toc.append("\n")
            }
            val tocPanel = Panel(
                renderable = toc,
                title = Text("Contents", style = Style(bold = true)),
                box = Box.ROUNDED,
                borderStyle = Style(color = Color.DarkGrey),
            )
            for (seg in tocPanel.render(tools.konsole.rich.Console.string(width = 30), tools.konsole.rich.RenderOptions(maxWidth = 30))) {
                text.append(seg.text, seg.style)
            }
            text.append("\n")
        }
        for (seg in RichMarkdown(markdown, codeTheme = codeTheme).render(
            tools.konsole.rich.Console.string(width = 80),
            tools.konsole.rich.RenderOptions(maxWidth = 80),
        )) {
            text.append(seg.text, seg.style)
        }
        return text
    }

    /** An entry in the table-of-contents extracted from `# Heading` markers. */
    public data class TocEntry(public val level: Int, public val title: String, public val line: Int)

    private fun extractToc(md: String): List<TocEntry> {
        val out = mutableListOf<TocEntry>()
        for ((i, line) in md.lines().withIndex()) {
            val match = Regex("^(#{1,6})\\s+(.+)$").matchEntire(line.trim()) ?: continue
            out += TocEntry(level = match.groupValues[1].length, title = match.groupValues[2].trim(), line = i + 1)
        }
        return out
    }
}
