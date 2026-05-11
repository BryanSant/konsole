package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.tree.Tree as RichTree
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/**
 * Interactive expandable tree widget. Mirrors Python textual's `Tree`.
 *
 * Each node carries an [id] and an arbitrary [data] payload of type [T];
 * nodes can be expanded/collapsed and the cursor navigated with arrow keys.
 *
 * Rendered via rich's [RichTree] renderable, walking only expanded branches.
 */
public open class Tree<T>(
    public val rootLabel: String,
    rootData: T? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public val root: Node<T> = Node(rootLabel, rootData, parent = null, depth = 0)

    /** Index of the currently highlighted node within the flattened visible list. */
    public var cursorLine: Int = 0
        private set

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("up", "cursor_up", show = false),
            Binding("down", "cursor_down", show = false),
            Binding("left", "collapse", "Collapse"),
            Binding("right", "expand", "Expand"),
            Binding("enter", "select", "Select"),
            Binding("space", "toggle_expand", show = false),
        )
    )

    public fun moveCursor(delta: Int) {
        val visible = visibleNodes()
        if (visible.isEmpty()) return
        cursorLine = (cursorLine + delta).coerceIn(0, visible.size - 1)
        post(Highlighted(this, visible[cursorLine]))
        refresh()
    }

    public fun currentNode(): Node<T>? = visibleNodes().getOrNull(cursorLine)

    public fun expandCurrent() {
        currentNode()?.let { if (it.children.isNotEmpty()) { it.expanded = true; refresh() } }
    }
    public fun collapseCurrent() {
        currentNode()?.let { if (it.expanded) { it.expanded = false; refresh() } }
    }
    public fun toggleExpand() {
        currentNode()?.let { it.expanded = !it.expanded; refresh() }
    }

    public fun selectCurrent(): Boolean {
        val node = currentNode() ?: return false
        return post(Selected(this, node))
    }

    private fun visibleNodes(): List<Node<T>> {
        val out = mutableListOf<Node<T>>()
        fun walk(n: Node<T>) {
            out += n
            if (n.expanded) for (c in n.children) walk(c)
        }
        walk(root)
        return out
    }

    override fun render(): Renderable {
        val visible = visibleNodes()
        val cursorNode = visible.getOrNull(cursorLine)
        // Build a rich Tree from the visible subtree. Highlight the cursor node.
        fun build(n: Node<T>): RichTree {
            val labelText = Text()
            val arrow = when {
                n.children.isEmpty() -> "  "
                n.expanded -> "▼ "
                else -> "▶ "
            }
            val baseStyle = if (n === cursorNode) Style(color = Color.Black, bgcolor = Color.Cyan, bold = true)
                            else if (hasFocus) Style.NULL else Style.NULL
            if (n.children.isNotEmpty()) {
                labelText.append(arrow, Style(color = Color.Cyan, bold = true))
            } else {
                labelText.append(arrow, Style.NULL)
            }
            labelText.append(n.label, baseStyle)
            val t = RichTree(labelText)
            if (n.expanded) {
                for (c in n.children) t.add(build(c).label)
            }
            return t
        }
        return build(root)
    }

    /** A node in the [Tree]. */
    public class Node<T>(
        public var label: String,
        public var data: T? = null,
        public val parent: Node<T>? = null,
        public val depth: Int = 0,
    ) {
        private val _children: MutableList<Node<T>> = mutableListOf()
        public val children: List<Node<T>> get() = _children
        public var expanded: Boolean = depth == 0  // root starts expanded

        public fun addLeaf(label: String, data: T? = null): Node<T> {
            val n = Node(label, data, this, depth + 1)
            _children += n
            return n
        }

        public fun add(label: String, data: T? = null): Node<T> = addLeaf(label, data)
    }

    public data class Highlighted<T>(val tree: Tree<T>, val node: Node<T>) : Message()
    public data class Selected<T>(val tree: Tree<T>, val node: Node<T>) : Message()
}
