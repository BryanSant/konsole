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
import tools.konsole.textual.widgets.TextArea
import tools.konsole.textual.widgets.Vertical

/**
 * A minimal Kotlin editor backed by the tree-sitter–powered [TextArea].
 * Keywords, types, strings, and numbers are highlighted using the default
 * dark ANSI theme.
 *
 *   ./demo.sh SyntaxEditor
 *
 * Move the cursor with arrow keys, type to insert, q is just the letter q
 * (it doesn't quit while editing) — use Ctrl+C to exit.
 */
public class SyntaxEditorApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val editor = TextArea(
        initial = INITIAL_SOURCE,
        language = "kotlin",
        showLineNumbers = true,
        id = "editor",
    )

    override val bindings = bindings("ctrl+c" to "quit")

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(
                Header(title = "Kotlin editor — tree-sitter syntax highlighting"),
                editor,
                Footer(bindings("Ctrl+C" to "quit")),
            ),
            heights = listOf(
                Scalar(1.0, LengthUnit.Cells),
                Scalar(1.0, LengthUnit.Fraction),
                Scalar(1.0, LengthUnit.Cells),
            ),
        )
    )

    override fun start() {
        super.start()
        if (focused !== editor) setFocus(editor)
    }
}

private val INITIAL_SOURCE = """
    package tools.konsole.examples

    import tools.konsole.rich.Text

    /** A small renderable function. */
    fun greet(name: String): Text {
        val message = "Hello, ${'$'}name!"
        val count = 42
        return Text(message).styled(color = "cyan", bold = true)
    }

    fun main() {
        val users = listOf("Ada", "Linus", "Grace")
        for (user in users) {
            println(greet(user))
        }
    }
""".trimIndent()

public fun main() {
    SyntaxEditorApp().run()
}
