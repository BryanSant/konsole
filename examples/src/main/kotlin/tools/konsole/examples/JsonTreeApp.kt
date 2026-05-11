package tools.konsole.examples

import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Tree
import tools.konsole.textual.widgets.Vertical

/**
 * Browse a JSON document as a [Tree]. Mirrors textual's `json_tree.py`.
 *
 *   ./gradlew :examples:runExample -Pexample=JsonTreeApp
 *
 * The Pilot script expands the root, walks down a few levels, and selects
 * a leaf so the demo exercises arrow + Right + Enter dispatch.
 */
public class JsonTreeApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    public val tree: Tree<Any?> = Tree("food.json", rootData = null, id = "tree")

    override val bindings = bindings(
        "q" to "quit",
        "ctrl+c" to "quit",
    )

    init {
        // Build a small JSON-like structure as a hierarchy.
        val data: Any = mapOf(
            "fruits" to listOf(
                mapOf("name" to "apple", "color" to "red", "calories" to 95),
                mapOf("name" to "banana", "color" to "yellow", "calories" to 105),
            ),
            "vegetables" to listOf(
                mapOf("name" to "carrot", "color" to "orange"),
                mapOf("name" to "broccoli", "color" to "green"),
            ),
            "drinks" to mapOf(
                "water" to "essential",
                "tea" to listOf("black", "green", "oolong"),
            ),
        )
        populate(tree.root, data)
        tree.root.expanded = true
    }

    private fun populate(node: Tree.Node<Any?>, value: Any?) {
        when (value) {
            is Map<*, *> -> {
                for ((k, v) in value) {
                    val keyLabel = when (v) {
                        is Map<*, *>, is List<*> -> "$k"
                        else -> "$k: $v"
                    }
                    val child = node.addLeaf(keyLabel, v)
                    populate(child, v)
                }
            }
            is List<*> -> {
                for ((i, item) in value.withIndex()) {
                    val label = when (item) {
                        is Map<*, *>, is List<*> -> "[$i]"
                        else -> "[$i] $item"
                    }
                    val child = node.addLeaf(label, item)
                    populate(child, item)
                }
            }
            else -> { /* leaf — no children */ }
        }
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(
                Header(title = "JSON Tree"),
                tree,
                Footer(this.bindings),
            ),
            heights = listOf(
                Scalar(1.0, LengthUnit.Cells),
                Scalar(1.0, LengthUnit.Fraction),
                Scalar(1.0, LengthUnit.Cells),
            ),
        )
    )
}

public fun main() {
    JsonTreeApp().run()
}
