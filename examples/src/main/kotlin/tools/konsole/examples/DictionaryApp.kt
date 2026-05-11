package tools.konsole.examples

import kotlinx.coroutines.runBlocking
import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.panel.Panel
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.pilot.Pilot
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Input
import tools.konsole.textual.widgets.Label
import tools.konsole.textual.widgets.OptionList
import tools.konsole.textual.widgets.Static
import tools.konsole.textual.widgets.WordListSuggester

private data class Entry(val word: String, val type: String, val definition: String)

private val SAMPLE_DICT: List<Entry> = listOf(
    Entry("konsole", "noun", "A pure-Kotlin port of Python's rich and textual libraries."),
    Entry("console", "noun", "A text-based interface used to interact with a computer."),
    Entry("renderable", "adjective", "Capable of being rendered to a display surface."),
    Entry("spinner", "noun", "An animated character sequence indicating in-progress work."),
    Entry("compose", "verb", "To declare a widget's children declaratively."),
    Entry("compositor", "noun", "The layered frame renderer that maps widgets to screen cells."),
    Entry("widget", "noun", "An interactive UI element in a TUI application."),
    Entry("driver", "noun", "The abstraction between a textual App and the underlying terminal."),
)

/**
 * Live-search dictionary. Type into the [Input] and the matching entries
 * filter in real time; pick one with arrow + Enter to see the definition.
 *
 *   ./gradlew :examples:runExample -Pexample=DictionaryApp
 *
 * The bundled Pilot script searches for "comp" and selects "compositor".
 */
public class DictionaryApp : App(HeadlessDriver()) {

    private val search = Input(
        placeholder = "search…",
        suggester = WordListSuggester(SAMPLE_DICT.map { it.word }),
        id = "search",
    )
    private val list = OptionList.ofLabels(
        SAMPLE_DICT.map { "${it.word}  [dim]— ${it.type}[/]" },
        id = "results",
    )
    private val detail = Static(
        defaultDetailPanel(SAMPLE_DICT.first()),
        id = "detail",
    )

    override val bindings = bindings(
        "q" to "quit",
        "/" to "focus_search",
        "escape" to "clear_search",
    )

    override fun compose(): Sequence<Widget> = sequenceOf(
        Header(title = "Dictionary"),
        search,
        list,
        detail,
        Footer(this.bindings),
    )

    init {
        search.start()
        list.start()
        // Filter the option list on every keystroke.
        search.onMessage<Input.Changed> { applyFilter(it.value) }
        list.onMessage<OptionList.Highlighted> { showEntry(it.index) }
        list.onMessage<OptionList.Selected> { showEntry(it.index) }
    }

    private fun applyFilter(query: String) {
        val matches = if (query.isBlank()) SAMPLE_DICT
                      else SAMPLE_DICT.filter { it.word.contains(query, ignoreCase = true) }
        if (matches.isNotEmpty()) {
            val first = matches.first()
            detail.update(defaultDetailPanel(first))
        }
        requestRefresh()
    }

    private fun showEntry(index: Int) {
        SAMPLE_DICT.getOrNull(index)?.let { detail.update(defaultDetailPanel(it)) }
        requestRefresh()
    }

    /** Test-visible accessor for the current shown definition. */
    public val detailContent: Any get() = detail.content
}

private fun defaultDetailPanel(e: Entry): Panel {
    val body = Text()
    body.append("${e.word}\n", Style(bold = true, color = Color.Cyan))
    body.append("${e.type}\n\n", Style(italic = true, color = Color.Yellow))
    body.append(e.definition)
    return Panel(
        renderable = body,
        title = Text("Definition", style = Style(bold = true)),
        box = Box.ROUNDED,
        borderStyle = Style(color = Color.Magenta),
    )
}

public fun main(): Unit = runBlocking {
    val app = DictionaryApp()
    val pilot = Pilot(app)
    pilot.use { p ->
        p.pause(100)
        app.renderFrame()

        // Type "comp" into the search box
        @Suppress("UNCHECKED_CAST")
        val input = pilot.findOne("#search") as? Input ?: error("missing #search")
        input.insert("comp")
        p.pause(50)
        app.renderFrame()

        // Find "compositor" in the list and highlight it
        @Suppress("UNCHECKED_CAST")
        val list = pilot.findOne("#results") as? OptionList ?: error("missing #results")
        // Walk to the "compositor" entry (index 5 in the original list)
        val compIdx = SAMPLE_DICT.indexOfFirst { it.word == "compositor" }
        if (compIdx >= 0) list.highlight(compIdx)
        list.selectCurrent()
        p.pause(50)
        app.renderFrame()
    }

    val console = Console.system()
    console.print("[bold]Dictionary demo finished.[/]")
    console.print("Selected definition panel rendered to driver (${(app.driver as HeadlessDriver).output.length} bytes).")
}
