package tools.konsole.examples

import kotlinx.coroutines.runBlocking
import tools.konsole.core.event.KeyCode
import tools.konsole.rich.Console
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Key
import tools.konsole.textual.pilot.Pilot
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Label
import tools.konsole.textual.widgets.TabPane
import tools.konsole.textual.widgets.TabbedContent

/**
 * Showcases [TabbedContent]. Three tabs ("Overview", "Stats", "About") cycle
 * through their content panes; arrow keys move the active tab.
 *
 *   ./gradlew :examples:runExample -Pexample=TabbedContentDemo
 *
 * The bundled Pilot script walks left/right through the tabs once, then
 * prints the final captured frame size.
 */
public class TabbedContentDemo : App(HeadlessDriver()) {

    private val tabbed = TabbedContent(
        panes = listOf(
            TabPane("overview", "Overview", listOf(
                Label("[bold cyan]Konsole[/]"),
                Label("Pure-Kotlin port of rich + textual on JLine FFM."),
                Label("Runs on Linux, macOS, Windows Terminal 1.25+."),
            )),
            TabPane("stats", "Stats", listOf(
                Label("[bold]Modules:[/] core · rich · textualize · examples"),
                Label("[bold]Tests:[/]   631 across the suite"),
                Label("[bold]Widgets:[/] 34 in the textualize catalog"),
            )),
            TabPane("about", "About", listOf(
                Label("Implemented by following the [italic]rich[/] and [italic]textual[/] sources."),
                Label("Python idioms adapted to Kotlin where the languages diverge."),
                Label("[dim]MIT licensed.[/]"),
            )),
        ),
        id = "tabs",
    )

    override val bindings = bindings(
        "q" to "quit",
        "left" to "previous_tab",
        "right" to "next_tab",
    )

    init {
        tabbed.tabs.start()
        // Forward arrow-key actions to the embedded Tabs widget.
        setFocus(tabbed.tabs)
    }

    @Suppress("unused") public fun action_previous_tab() { tabbed.tabs.previousTab(); requestRefresh() }
    @Suppress("unused") public fun action_next_tab() { tabbed.tabs.nextTab(); requestRefresh() }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Header(title = "TabbedContent demo"),
        tabbed,
        Footer(this.bindings),
    )

    public val activeTabId: String? get() = tabbed.tabs.activeTabId
}

public fun main(): Unit = runBlocking {
    val app = TabbedContentDemo()
    val pilot = Pilot(app)
    pilot.use { p ->
        p.pause(100)
        app.renderFrame()

        val driver = app.driver as HeadlessDriver
        // Cycle through every tab using the Right binding.
        repeat(3) {
            driver.send(Key(KeyCode.Right))
            p.pause(40)
            app.renderFrame()
        }
        // And one Left for good measure.
        driver.send(Key(KeyCode.Left))
        p.pause(40)
        app.renderFrame()
    }

    val console = Console.system()
    val driver = app.driver as HeadlessDriver
    console.print("[bold]TabbedContent demo finished.[/]")
    console.print("Final active tab: [bold cyan]${app.activeTabId}[/]")
    console.print("Captured ${driver.output.length} bytes of ANSI.")
}
