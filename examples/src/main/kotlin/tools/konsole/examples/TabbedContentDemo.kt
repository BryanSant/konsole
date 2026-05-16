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
import tools.konsole.textual.widgets.Label
import tools.konsole.textual.widgets.TabPane
import tools.konsole.textual.widgets.TabbedContent
import tools.konsole.textual.widgets.Vertical

/**
 * Showcases [TabbedContent]. Three tabs ("Overview", "Stats", "About") cycle
 * through their content panes; arrow keys move the active tab.
 *
 *   ./gradlew :examples:runExample -Pexample=TabbedContentDemo
 *
 * The bundled Pilot script walks left/right through the tabs once, then
 * prints the final captured frame size.
 */
public class TabbedContentDemo(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val tabbed = TabbedContent(
        panes = listOf(
            TabPane("overview", "Overview", listOf(
                Label("[bold cyan]Konsole[/]"),
                Label("Pure-Kotlin port of rich + textual on direct FFM termios."),
                Label("Runs on Linux and macOS today; Windows driver TBD."),
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
        "ctrl+c" to "quit",
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
        Vertical(
            children = listOf(
                Header(title = "TabbedContent demo"),
                tabbed,
                Footer(this.bindings),
            ),
            heights = listOf(
                Scalar(1.0, LengthUnit.Cells),
                Scalar(1.0, LengthUnit.Fraction),
                Scalar(1.0, LengthUnit.Cells),
            ),
        )
    )

    public val activeTabId: String? get() = tabbed.tabs.activeTabId
}

public fun main() {
    TabbedContentDemo().run()
}
