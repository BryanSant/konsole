package tools.konsole.examples

import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Checkbox
import tools.konsole.textual.widgets.Collapsible
import tools.konsole.textual.widgets.DataTable
import tools.konsole.textual.widgets.Digits
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.HelpPanel
import tools.konsole.textual.widgets.Input
import tools.konsole.textual.widgets.Label
import tools.konsole.textual.widgets.Link
import tools.konsole.textual.widgets.ListItem
import tools.konsole.textual.widgets.ListView
import tools.konsole.textual.widgets.LoadingIndicator
import tools.konsole.textual.widgets.Log
import tools.konsole.textual.widgets.MaskedInput
import tools.konsole.textual.widgets.OptionList
import tools.konsole.textual.widgets.Placeholder
import tools.konsole.textual.widgets.PrettyWidget
import tools.konsole.textual.widgets.ProgressBar
import tools.konsole.textual.widgets.RadioButton
import tools.konsole.textual.widgets.RadioSet
import tools.konsole.textual.widgets.RichLog
import tools.konsole.textual.widgets.Rule
import tools.konsole.textual.widgets.Select
import tools.konsole.textual.widgets.SelectionList
import tools.konsole.textual.widgets.Sparkline
import tools.konsole.textual.widgets.Static
import tools.konsole.textual.widgets.Switch
import tools.konsole.textual.widgets.Toast
import tools.konsole.textual.widgets.ToastSeverity
import tools.konsole.textual.widgets.Tooltip
import tools.konsole.textual.widgets.Tree
import tools.konsole.textual.widgets.Welcome

/**
 * Comprehensive gallery showing every shipped widget. Mirrors textual's
 * `demo` app in spirit — a single screen that exercises the whole catalog.
 *
 *   ./gradlew :examples:runExample -Pexample=WidgetGallery
 *
 * The Pilot script tabs through the focusable widgets, demonstrates the
 * focus highlight + hover/press states, fires a Toast, then prints the
 * final captured frame size.
 */
public class WidgetGallery(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    // --- Display widgets ---
    private val display = Digits("12:34", id = "digits")
    private val label = Label("[bold]Label[/]   plain content via Static", id = "label")
    private val rule = Rule(title = "section divider", char = '─', id = "rule")
    private val progress = ProgressBar(total = 100.0, progress = 67.0, id = "progress")
    private val loadingIndicator = LoadingIndicator(spinnerName = "dots", id = "loading")
    private val spark = Sparkline(listOf(1.0, 4.0, 2.0, 8.0, 5.0, 9.0, 3.0, 7.0, 6.0), id = "spark")

    // --- Input widgets ---
    private val button = Button("Press me", variant = ButtonVariant.Primary, id = "btn")
    private val toggleSwitch = Switch(initial = false, id = "switch")
    private val check = Checkbox(label = "Accept terms", id = "check")
    private val radio = RadioSet(
        RadioButton("Apple", initial = true),
        RadioButton("Banana"),
        RadioButton("Cherry"),
        id = "radio",
    )
    private val textInput = Input(placeholder = "Type here…", id = "input")
    private val maskedInput = MaskedInput(template = "AA99 9AA", id = "masked")

    // --- List widgets ---
    private val optionList = OptionList.ofLabels(
        listOf("Option A", "Option B", "Option C"),
        id = "options",
    )
    private val selectionList = SelectionList(
        listOf(
            Triple("r", "Red", false),
            Triple("g", "Green", true),
            Triple("b", "Blue", false),
        ),
        id = "selection",
    )
    private val select = Select(
        options = listOf("k" to "Kotlin", "j" to "Java", "p" to "Python"),
        initialSelected = "k",
        id = "select",
    )
    private val listView = ListView(
        items = listOf(
            ListItem(Label("Item one")),
            ListItem(Label("Item two")),
            ListItem(Label("Item three")),
        ),
        id = "listview",
    )

    // --- Tree / Data ---
    private val tree = Tree<String>("/", rootData = "/", id = "tree").apply {
        root.expanded = true
        val src = root.addLeaf("src/")
        src.addLeaf("Main.kt"); src.addLeaf("Util.kt")
        root.addLeaf("README.md")
    }
    private val dataTable = DataTable(id = "table").apply {
        addColumn("Name"); addColumn("Score"); addColumn("Status")
        addRow("Alice", "99", "[green]passing[/]")
        addRow("Bob", "73", "[yellow]pending[/]")
        addRow("Carol", "21", "[red]failing[/]")
    }

    // --- Text + log ---
    private val richLog = RichLog().apply {
        write("[bold cyan]rich log[/] supports markup")
        write("[red]error[/] entries highlight")
    }
    private val plainLog = Log().apply {
        write("plain log line 1")
        write("plain log line 2 (no markup)")
    }

    // --- Misc / decorative ---
    private val placeholder = Placeholder(label = "sidebar slot", id = "ph")
    private val welcome = Welcome(title = "Konsole gallery", body = "Press [b]q[/] to quit.")
    private val pretty = PrettyWidget(target = mapOf("ok" to true, "score" to 9.5), id = "pretty")
    private val collapsible = Collapsible(
        title = "More details",
        collapsed = false,
        contents = listOf(Label("These rows fold away when collapsed.")),
        id = "collapsible",
    )

    // --- Chrome ---
    private val link = Link(label = "konsole on GitHub", url = "https://github.com/bryansant/konsole", id = "link")

    override val bindings = bindings(
        "q" to "quit",
        "ctrl+c" to "quit",
        "tab" to "focus_next",
        "shift+tab" to "focus_previous",
        "ctrl+t" to "show_toast",
    )

    @Suppress("unused")
    public fun action_show_toast() {
        notify(Toast(title = "Hello", message = "from konsole", severity = ToastSeverity.Information, timeout = 1500L))
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Header(title = "Konsole widget gallery"),
        welcome,
        rule,
        label, display, progress, loadingIndicator, spark,
        button, toggleSwitch, check,
        radio,
        textInput, maskedInput,
        optionList, selectionList, select, listView,
        tree, dataTable,
        richLog, plainLog,
        placeholder, pretty, collapsible, link,
        Footer(this.bindings),
    )
}

public fun main() {
    WidgetGallery().run()
}
