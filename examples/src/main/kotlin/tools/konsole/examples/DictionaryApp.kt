package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.panel.Panel
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Input
import tools.konsole.textual.widgets.OptionList
import tools.konsole.textual.widgets.Static
import tools.konsole.textual.widgets.Vertical
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
 * filter in real time; arrow-key + Enter (or click) to see the definition.
 *
 *   ./demo.sh Dictionary
 *
 * Keys: `/` focuses the search box, Esc clears it, q or Ctrl+C quits.
 */
public class DictionaryApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

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
        "ctrl+c" to "quit",
        "/" to "focus_search",
        "escape" to "clear_search",
    )

    @Suppress("unused") public fun action_focus_search() { setFocus(search) }
    @Suppress("unused") public fun action_clear_search() {
        search.clear()
        applyFilter("")
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(
                Header(title = "Dictionary"),
                search,
                list,
                detail,
                Footer(this.bindings),
            ),
            heights = listOf(
                Scalar(1.0, LengthUnit.Cells),       // header
                Scalar(1.0, LengthUnit.Cells),       // search input
                Scalar(SAMPLE_DICT.size.toDouble(), LengthUnit.Cells),  // option list
                Scalar(1.0, LengthUnit.Fraction),    // detail panel fills the rest
                Scalar(1.0, LengthUnit.Cells),       // footer
            ),
        )
    )

    init {
        search.start()
        list.start()
        // Filter the option list on every keystroke.
        search.onMessage<Input.Changed> { applyFilter(it.value) }
        list.onMessage<OptionList.Highlighted> { showEntry(it.index) }
        list.onMessage<OptionList.Selected> { showEntry(it.index) }
    }

    override fun start() {
        super.start()
        if (focused !== search) setFocus(search)
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

public fun main() {
    DictionaryApp().run()
}
