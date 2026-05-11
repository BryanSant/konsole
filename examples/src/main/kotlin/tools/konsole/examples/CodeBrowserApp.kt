package tools.konsole.examples

import java.io.File
import kotlinx.coroutines.runBlocking
import tools.konsole.core.event.KeyCode
import tools.konsole.rich.Console
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Key
import tools.konsole.textual.pilot.Pilot
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.DirectoryTree
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Log

/**
 * Filesystem explorer with a [DirectoryTree] sidebar and a [Log] preview
 * pane. Mirrors textual's `code_browser.py`.
 *
 *   ./gradlew :examples:runExample -Pexample=CodeBrowserApp
 *
 * The Pilot script navigates into the konsole project root and previews
 * the README.md content.
 */
public class CodeBrowserApp(rootPath: String) : App(HeadlessDriver()) {

    public val tree: DirectoryTree = DirectoryTree(rootPath, id = "tree")
    public val preview: Log = Log(maxLines = 200, id = "preview")

    override val bindings = bindings(
        "q" to "quit",
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
        Header(title = "Code browser: ${File(tree.rootPath).name}"),
        tree,
        preview,
        Footer(this.bindings),
    )
}

public fun main(): Unit = runBlocking {
    val rootPath = System.getProperty("user.dir")
    val app = CodeBrowserApp(rootPath)
    val pilot = Pilot(app)
    pilot.use { p ->
        p.pause(100)
        app.renderFrame()

        // Walk to README.md if present.
        val readmeNode = app.tree.root.children.firstOrNull { (it.data as? File)?.name == "README.md" }
        if (readmeNode != null) {
            // Highlight the README; the Tree.Highlighted message wires the preview.
            val idx = app.tree.root.children.indexOf(readmeNode) + 1  // +1 for root row
            repeat(idx) {
                (app.driver as HeadlessDriver).send(Key(KeyCode.Down))
                p.pause(15)
            }
            (app.driver as HeadlessDriver).send(Key(KeyCode.Enter))
            p.pause(80)
            app.renderFrame()
        }
    }

    val console = Console.system()
    val driver = app.driver as HeadlessDriver
    console.print("[bold]Code browser demo finished.[/]")
    console.print("Preview pane has [cyan]${app.preview.size}[/] lines loaded.")
    console.print("Captured ${driver.output.length} bytes of ANSI.")
}
