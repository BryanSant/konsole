package tools.konsole.examples

import java.io.File
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.DirectoryTree
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Horizontal
import tools.konsole.textual.widgets.Log
import tools.konsole.textual.widgets.Vertical

/**
 * Filesystem explorer with a [DirectoryTree] sidebar and a [Log] preview
 * pane. Mirrors textual's `code_browser.py`.
 *
 *   ./gradlew :examples:runExample -Pexample=CodeBrowserApp
 *
 * The Pilot script navigates into the konsole project root and previews
 * the README.md content.
 */
public class CodeBrowserApp(
    rootPath: String,
    headless: Boolean = false,
) : App(if (headless) HeadlessDriver() else systemDriver()) {

    public val tree: DirectoryTree = DirectoryTree(rootPath, id = "tree")
    public val preview: Log = Log(maxLines = 200, id = "preview")

    override val bindings = bindings(
        "q" to "quit",
        "ctrl+c" to "quit",
        "enter" to "open_file",
    )

    init {
        tree.start()
        // Update preview whenever the highlighted Tree node changes.
        tree.onMessage<tools.konsole.textual.widgets.Tree.Highlighted<*>> { ev ->
            val data = ev.node.data
            if (data is File && data.isFile) loadFile(data)
        }
        tree.onMessage<tools.konsole.textual.widgets.Tree.Selected<*>> { ev ->
            val data = ev.node.data
            if (data is File && data.isFile) loadFile(data)
        }
    }

    @Suppress("unused")
    public fun action_open_file() {
        val data = tree.currentNode()?.data
        if (data is File && data.isFile) loadFile(data)
    }

    private fun loadFile(file: File) {
        preview.clear()
        try {
            file.useLines { lines ->
                for (line in lines.take(200)) preview.write(line)
            }
        } catch (e: Throwable) {
            preview.write("[error: ${e.message}]")
        }
        requestRefresh()
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(
                Header(title = "Code browser: ${File(tree.rootPath).name}"),
                // Split body: tree sidebar on left, preview pane on right.
                Horizontal(
                    children = listOf(tree, preview),
                    widths = listOf(
                        Scalar(30.0, LengthUnit.Cells),       // sidebar
                        Scalar(1.0, LengthUnit.Fraction),     // preview fills the rest
                    ),
                ),
                Footer(this.bindings),
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
        if (focused !== tree) setFocus(tree)
    }
}

public fun main() {
    val rootPath = System.getProperty("user.dir")
    CodeBrowserApp(rootPath).run()
}
