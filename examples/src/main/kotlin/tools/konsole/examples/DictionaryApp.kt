package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.table.Column
import tools.konsole.rich.table.Table
import tools.konsole.textual.app.App
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Label

/**
 * Phase 11 textual demo — a small Dictionary app inspired by the textual API-call tutorial.
 *
 *   ./gradlew :examples:runExample -Pexample=DictionaryApp
 *
 * The textual original fetches definitions from an HTTP API and renders them
 * live. Konsole's Worker / async wiring exists (Phase 7) but the live-render
 * pipeline isn't done yet (Phase 9.5+), so this demo bakes a few sample
 * entries into a Table.
 */
public class DictionaryApp : App(HeadlessDriver()) {
    public val title: Label = Label("[bold]Dictionary[/]", id = "title")
    init {
        attach(Header(title = "Dictionary"))
        attach(title)
    }
}

private val SAMPLES: Map<String, Pair<String, String>> = linkedMapOf(
    "konsole" to ("noun" to "A pure-Kotlin port of Python's rich and textual libraries."),
    "console" to ("noun" to "A text-based interface used to interact with a computer."),
    "renderable" to ("adjective" to "Capable of being rendered to a display surface."),
    "spinner" to ("noun" to "An animated character sequence indicating in-progress work."),
)

public fun main() {
    val console = Console.system()
    val app = DictionaryApp()

    console.print(Panel(app.title.render(), box = Box.HEAVY, borderStyle = Style(color = Color.Magenta)))

    val table = Table(box = Box.SQUARE, title = Text("Sample entries", style = Style(bold = true)))
    table.addColumn(Column(header = Text("Word", style = Style(bold = true, color = Color.Cyan))))
    table.addColumn(Column(header = Text("Type", style = Style(bold = true, color = Color.Yellow))))
    table.addColumn(Column(header = Text("Definition", style = Style(bold = true))))
    for ((word, def) in SAMPLES) {
        table.addRow(Text(word, style = Style(color = Color.Cyan)), Text(def.first, style = Style(italic = true)), Text(def.second))
    }
    console.print(table)
}
