package tools.konsole.textual.dom

import tools.konsole.textual.message.MessagePump
import tools.konsole.textual.reactive.Reactable

/**
 * Base class for everything in the textual DOM tree. Mirrors Python textual's `DOMNode`.
 *
 * Each node has a stable [id], a set of CSS [classes], a [parent] reference (set
 * during mount), and a list of [children]. Walks are provided for traversal,
 * and selector matching plugs into [tools.konsole.textual.dom.Selector].
 */
public abstract class DOMNode(
    public val id: String? = null,
    classes: Set<String> = emptySet(),
) : MessagePump(), Reactable {

    /** CSS-like classes attached to this node. Mutable so tests/widgets can add/remove. */
    public val classes: MutableSet<String> = classes.toMutableSet()

    /** Optional name (e.g. for logging / dev console). */
    public open val name: String? = null

    /** Parent in the DOM tree, or `null` if unattached. Set by [tools.konsole.textual.widget.Widget.mount]. */
    public var parent: DOMNode? = null
        internal set

    /** Children attached to this node. */
    private val _children: MutableList<DOMNode> = mutableListOf()
    public val children: List<DOMNode> get() = _children.toList()

    /** All ancestors from immediate parent up to the root, exclusive of `this`. */
    public val ancestors: Sequence<DOMNode>
        get() = generateSequence(parent) { it.parent }

    /** All descendants in pre-order, excluding `this`. */
    public val descendants: Sequence<DOMNode>
        get() = sequence {
            for (c in _children) {
                yield(c)
                yieldAll(c.descendants)
            }
        }

    /** Attach [node] as a child (sets `node.parent = this`). */
    public open fun attach(node: DOMNode) {
        node.parent = this
        _children += node
    }

    /** Detach [node]. */
    public open fun detach(node: DOMNode) {
        _children.remove(node)
        node.parent = null
    }

    /** All nodes in the subtree rooted at this node (pre-order, this first). */
    public fun walk(): Sequence<DOMNode> = sequence {
        yield(this@DOMNode)
        yieldAll(descendants)
    }

    /** CSS-selector-style name → matcher key. Override for synthetic types. */
    public open val cssType: String get() = this::class.simpleName ?: "DOMNode"

    override fun toString(): String =
        "${this::class.simpleName}(id=$id, classes=${classes.joinToString(",")})"
}
