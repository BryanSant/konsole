package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.markdown.Markdown as RichMarkdown
import tools.konsole.rich.syntax.SyntaxTheme
import tools.konsole.textual.widget.Scrollable
import tools.konsole.textual.widget.Widget

/**
 * Renders Markdown text. Mirrors Python textual's `Markdown` widget.
 *
 * Wraps rich's [RichMarkdown] renderable (which itself uses commonmark-java
 * + the GFM tables extension) and adds [Scrollable] behavior so long docs
 * can be browsed inside a viewport.
 *
 * @param markdown initial Markdown source.
 * @param codeTheme syntax theme for fenced code blocks.
 */
public open class Markdown(
    markdown: String = "",
    public val codeTheme: SyntaxTheme = SyntaxTheme.MONOKAI,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Scrollable {

    public var markdown: String = markdown
        private set

    override var scrollX: Int = 0
    override var scrollY: Int = 0
    override val contentWidth: Int get() = 80
    override val contentHeight: Int get() = markdown.lines().size

    /** Replace the rendered Markdown source. */
    public fun update(markdown: String) {
        this.markdown = markdown
        refresh()
    }

    override fun render(): Renderable = RichMarkdown(markdown, codeTheme = codeTheme)
}
