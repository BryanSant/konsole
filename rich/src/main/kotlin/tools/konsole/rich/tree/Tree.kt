package tools.konsole.rich.tree

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.layout.collectLines
import tools.konsole.rich.markup.Markup

/** Box-drawing characters used to render guide lines. */
public data class TreeGuides(
    public val space: String = "    ",
    public val branch: String = "│   ",
    public val tee: String = "├── ",
    public val last: String = "└── ",
) {
    public companion object {
        public val NORMAL: TreeGuides = TreeGuides()
        public val ASCII: TreeGuides = TreeGuides(space = "    ", branch = "|   ", tee = "+-- ", last = "`-- ")
        public val BOLD: TreeGuides = TreeGuides(space = "    ", branch = "┃   ", tee = "┣━━ ", last = "┗━━ ")
        public val DOUBLE: TreeGuides = TreeGuides(space = "    ", branch = "║   ", tee = "╠══ ", last = "╚══ ")
    }
}

/**
 * A tree of [Renderable] labels. Mirrors `rich.tree.Tree`.
 *
 * Build a tree by calling [add] which returns the new child for chaining.
 */
public class Tree(
    public val label: Renderable,
    public val style: Style = Style.NULL,
    public val guideStyle: Style = Style.NULL,
    public val expanded: Boolean = true,
    public val highlight: Boolean = false,
    public val hideRoot: Boolean = false,
    public val guides: TreeGuides = TreeGuides.NORMAL,
) : Measurable {

    private val children: MutableList<Tree> = mutableListOf()

    /** Add a child node and return it for chaining. */
    public fun add(label: Renderable, style: Style? = null, guideStyle: Style? = null): Tree {
        val child = Tree(
            label = label,
            style = style ?: this.style,
            guideStyle = guideStyle ?: this.guideStyle,
            expanded = expanded,
            highlight = highlight,
            guides = guides,
        )
        children += child
        return child
    }

    /** Add a child from a markup string and return it for chaining. */
    public fun add(label: String, style: Style? = null): Tree =
        add(Markup.parse(label), style)

    /** Number of immediate children. */
    public val childCount: Int get() = children.size

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        renderInto(this@Tree, console, options, prefix = listOf(), isLast = true, isRoot = true)
    }

    private suspend fun SequenceScope<Segment>.renderInto(
        node: Tree,
        console: Console,
        options: RenderOptions,
        prefix: List<String>,
        isLast: Boolean,
        isRoot: Boolean,
    ) {
        val labelStyle = if (node.style.isNull) null else node.style
        val guideS = if (node.guideStyle.isNull) null else node.guideStyle

        if (!isRoot || !node.hideRoot) {
            // Emit prefix segments (ancestor guides) then this node's connector.
            val connector = if (isRoot && !node.hideRoot) "" else if (isLast) node.guides.last else node.guides.tee
            val totalPrefixCells = prefix.sumOf { it.length } + connector.length
            val labelWidth = (options.maxWidth - totalPrefixCells).coerceAtLeast(1)

            val labelLines = collectLines(node.label.render(console, options.withMaxWidth(labelWidth)))
            for ((i, line) in labelLines.withIndex()) {
                if (i > 0) yield(Segment.LINE)
                if (i == 0) {
                    for (p in prefix) if (p.isNotEmpty()) yield(Segment(p, guideS))
                    if (connector.isNotEmpty()) yield(Segment(connector, guideS))
                } else {
                    // Continuation line: replace connectors with branch/space
                    for (p in prefix) if (p.isNotEmpty()) yield(Segment(p, guideS))
                    val cont = if (isLast) node.guides.space else node.guides.branch
                    yield(Segment(cont, guideS))
                }
                if (labelStyle != null) {
                    for (s in line.segments) yield(if (s.style == null) Segment(s.text, labelStyle) else s)
                } else {
                    for (s in line.segments) yield(s)
                }
            }
        }

        if (!node.expanded || node.children.isEmpty()) return
        // Guide string for descendants below this node:
        //   if root and hidden → no extra guide
        //   if isLast → space (no continuing branch under us)
        //   else → branch (continuing branch from a tee above us)
        val nextPrefix: List<String> = when {
            isRoot && node.hideRoot -> prefix
            isLast -> prefix + node.guides.space
            else -> prefix + node.guides.branch
        }

        for ((i, child) in node.children.withIndex()) {
            yield(Segment.LINE)
            renderInto(child, console, options, nextPrefix, isLast = (i == node.children.lastIndex), isRoot = false)
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        // Conservative: full width.
        return Measurement(0, options.maxWidth)
    }
}

public fun tree(label: String, configure: Tree.() -> Unit = {}): Tree =
    Tree(Markup.parse(label)).apply(configure)
