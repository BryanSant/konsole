package tools.konsole.rich.markdown

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.layout.Rule
import tools.konsole.rich.syntax.Syntax
import tools.konsole.rich.syntax.SyntaxTheme
import tools.konsole.core.style.Color
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import tools.konsole.rich.box.Box
import tools.konsole.rich.table.Column
import tools.konsole.rich.table.Table

/**
 * Renders Markdown text as a rich [Renderable]. Mirrors `rich.markdown.Markdown`.
 *
 * Uses commonmark-java to parse the AST, then walks it producing konsole-rich segments.
 * Code blocks render as [Syntax]; everything else maps to styled [Text] / [Rule].
 */
public class Markdown(
    public val markdown: String,
    public val codeTheme: SyntaxTheme = SyntaxTheme.MONOKAI,
    public val justify: tools.konsole.rich.text.Justify = tools.konsole.rich.text.Justify.Default,
    public val style: Style = Style.NULL,
) : Measurable {

    private val document: Node by lazy {
        Parser.builder()
            .extensions(listOf(TablesExtension.create()))
            .build()
            .parse(markdown)
    }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> {
        val state = State(console, options)
        renderNode(state, document, top = true)
        return state.flush().asSequence()
    }

    override fun measure(console: Console, options: RenderOptions): Measurement =
        Measurement(0, options.maxWidth)

    /** Mutable rendering state — tracks emitted segments and text accumulators between blocks. */
    private class State(val console: Console, val options: RenderOptions) {
        val out: MutableList<Segment> = mutableListOf()
        var firstBlock = true

        fun emitBlankLine() { out += Segment.LINE; out += Segment.LINE }
        fun emit(seg: Segment) { out += seg }
        fun emitText(s: String, style: Style? = null) { out += Segment(s, style) }
        fun emitLine() { out += Segment.LINE }

        fun flush(): List<Segment> = out
    }

    private fun renderNode(state: State, node: Node, top: Boolean = false, indent: String = "", inheritedStyle: Style = Style.NULL) {
        when (node) {
            is Document -> {
                var child = node.firstChild
                var first = true
                while (child != null) {
                    if (!first) state.emitBlankLine()
                    renderNode(state, child, top = true, indent = indent, inheritedStyle = inheritedStyle)
                    first = false
                    child = child.next
                }
            }
            is Heading -> {
                val style = headingStyle(node.level, state.console)
                val prefix = "#".repeat(node.level) + " "
                state.emit(Segment(prefix, Style(color = Color.Yellow)))
                renderInline(state, node, style)
                if (node.level == 1) {
                    state.emitLine()
                    val rule = Rule(style = Style(color = Color.Yellow), char = '─')
                    val width = state.options.maxWidth
                    state.emit(Segment("─".repeat(width), Style(color = Color.Yellow)))
                }
            }
            is Paragraph -> {
                renderInline(state, node, inheritedStyle)
            }
            is BulletList -> {
                var item = node.firstChild
                var first = true
                while (item != null) {
                    if (!first) state.emitLine()
                    state.emit(Segment(indent, null))
                    state.emit(Segment("• ", Style(color = Color.Yellow, bold = true)))
                    renderInline(state, item, inheritedStyle)
                    first = false
                    item = item.next
                }
            }
            is OrderedList -> {
                var item = node.firstChild
                var i = node.markerStartNumber ?: 1
                var first = true
                while (item != null) {
                    if (!first) state.emitLine()
                    state.emit(Segment(indent, null))
                    state.emit(Segment("$i. ", Style(color = Color.Yellow, bold = true)))
                    renderInline(state, item, inheritedStyle)
                    i += 1
                    first = false
                    item = item.next
                }
            }
            is ListItem -> {
                renderInline(state, node, inheritedStyle)
            }
            is BlockQuote -> {
                val quoteStyle = state.console.theme["markdown.block_quote"] ?: Style(color = Color.Magenta)
                var child = node.firstChild
                var first = true
                while (child != null) {
                    if (!first) state.emitBlankLine()
                    state.emit(Segment("│ ", quoteStyle))
                    renderInline(state, child, quoteStyle)
                    first = false
                    child = child.next
                }
            }
            is FencedCodeBlock -> {
                val info = node.info ?: ""
                val lang = info.substringBefore(' ').ifEmpty { "text" }
                val syntax = Syntax(node.literal.trimEnd('\n'), lang, theme = codeTheme)
                for (s in syntax.render(state.console, state.options)) state.emit(s)
            }
            is IndentedCodeBlock -> {
                val syntax = Syntax(node.literal.trimEnd('\n'), "text", theme = codeTheme)
                for (s in syntax.render(state.console, state.options)) state.emit(s)
            }
            is HtmlBlock -> {
                state.emitText(node.literal.trimEnd('\n'), Style(dim = true))
            }
            is ThematicBreak -> {
                state.emit(Segment("─".repeat(state.options.maxWidth), Style(color = Color.Yellow)))
            }
            is TableBlock -> {
                renderTable(state, node)
            }
            else -> {
                renderInline(state, node, inheritedStyle)
            }
        }
    }

    private fun renderInline(state: State, parent: Node, inheritedStyle: Style) {
        var child = parent.firstChild
        while (child != null) {
            renderInlineNode(state, child, inheritedStyle)
            child = child.next
        }
    }

    /** Render a GFM-extension table. Mirrors rich's `markdown.TableElement`. */
    private fun renderTable(state: State, node: TableBlock) {
        // Collect cells by row, separating head and body.
        val headerCells = mutableListOf<String>()
        val bodyRows = mutableListOf<List<String>>()
        var section = node.firstChild
        while (section != null) {
            when (section) {
                is TableHead -> {
                    val row = section.firstChild as? TableRow
                    if (row != null) {
                        var cell = row.firstChild
                        while (cell != null) {
                            if (cell is TableCell) headerCells += inlineToString(cell)
                            cell = cell.next
                        }
                    }
                }
                is TableBody -> {
                    var row = section.firstChild
                    while (row != null) {
                        if (row is TableRow) {
                            val cells = mutableListOf<String>()
                            var cell = row.firstChild
                            while (cell != null) {
                                if (cell is TableCell) cells += inlineToString(cell)
                                cell = cell.next
                            }
                            bodyRows += cells
                        }
                        row = row.next
                    }
                }
            }
            section = section.next
        }
        val table = Table(box = Box.SQUARE)
        for (header in headerCells) table.addColumn(Column(header = tools.konsole.rich.Text(header)))
        for (row in bodyRows) {
            val cells: Array<tools.konsole.rich.Renderable> = row.map { tools.konsole.rich.Text(it) as tools.konsole.rich.Renderable }.toTypedArray()
            table.addRow(*cells)
        }
        for (s in table.render(state.console, state.options)) state.emit(s)
    }

    private fun inlineToString(node: Node): String {
        val sb = StringBuilder()
        var child: Node? = node.firstChild
        while (child != null) {
            when (child) {
                is org.commonmark.node.Text -> sb.append(child.literal)
                is Code -> sb.append(child.literal)
                is SoftLineBreak, is HardLineBreak -> sb.append(' ')
                else -> {
                    // Inline emphasis etc — flatten by descending
                    var sub: Node? = child.firstChild
                    while (sub != null) {
                        if (sub is org.commonmark.node.Text) sb.append(sub.literal)
                        sub = sub.next
                    }
                }
            }
            child = child.next
        }
        return sb.toString().trim()
    }

    private fun renderInlineNode(state: State, node: Node, inheritedStyle: Style) {
        when (node) {
            is org.commonmark.node.Text -> state.emitText(node.literal, if (inheritedStyle.isNull) null else inheritedStyle)
            is StrongEmphasis -> renderInline(state, node, inheritedStyle + Style(bold = true))
            is Emphasis -> renderInline(state, node, inheritedStyle + Style(italic = true))
            is Code -> {
                val codeStyle = state.console.theme["markdown.code"] ?: Style(color = Color.Cyan, bgcolor = Color.Black)
                state.emit(Segment(node.literal, codeStyle))
            }
            is Link -> {
                val linkStyle = state.console.theme["markdown.link"] ?: Style(color = Color.Blue, underline = true)
                val merged = inheritedStyle + linkStyle.copy(link = node.destination)
                renderInline(state, node, merged)
            }
            is Image -> {
                val text = (node.firstChild as? org.commonmark.node.Text)?.literal ?: node.title ?: "image"
                state.emitText("[image: $text → ${node.destination}]", Style(color = Color.Magenta, italic = true))
            }
            is HardLineBreak -> state.emitLine()
            is SoftLineBreak -> state.emitText(" ")
            is HtmlInline -> state.emitText(node.literal, Style(dim = true))
            is Paragraph -> renderInline(state, node, inheritedStyle)
            is BulletList, is OrderedList, is BlockQuote, is FencedCodeBlock, is IndentedCodeBlock, is ThematicBreak -> {
                state.emitLine()
                renderNode(state, node, inheritedStyle = inheritedStyle)
            }
            else -> renderInline(state, node, inheritedStyle)
        }
    }

    private fun headingStyle(level: Int, console: Console): Style =
        console.theme["markdown.h$level"] ?: when (level) {
            1 -> Style(color = Color.Yellow, bold = true)
            2 -> Style(color = Color.Yellow, bold = true, underline = true)
            3 -> Style(color = Color.Yellow, bold = true)
            4 -> Style(color = Color.Yellow, bold = true, dim = true)
            5 -> Style(color = Color.Yellow, underline = true)
            else -> Style(italic = true)
        }
}
