package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Align
import tools.konsole.rich.layout.Columns
import tools.konsole.rich.layout.Group
import tools.konsole.rich.layout.Padded
import tools.konsole.rich.layout.Padding
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.text.Justify
import tools.konsole.textual.app.App
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Label

/**
 * Phase 11 textual demo — the canonical Calculator app from the textual docs,
 * adapted to konsole's Phase 9 widget set.
 *
 *   ./gradlew :examples:runExample -Pexample=CalculatorApp
 *
 * The full live-update arithmetic engine arrives in Phase 9.5+; this demo
 * renders the static UI to show the layout works.
 */
public class CalculatorApp : App(HeadlessDriver()) {
    public val display: Label = Label("0", id = "display")
    public val buttons: List<Button> = listOf(
        Button("AC", variant = ButtonVariant.Error, id = "ac"),
        Button("±", id = "negate"),
        Button("%", id = "percent"),
        Button("÷", variant = ButtonVariant.Warning, id = "divide"),
        Button("7", id = "7"), Button("8", id = "8"), Button("9", id = "9"),
        Button("×", variant = ButtonVariant.Warning, id = "multiply"),
        Button("4", id = "4"), Button("5", id = "5"), Button("6", id = "6"),
        Button("-", variant = ButtonVariant.Warning, id = "minus"),
        Button("1", id = "1"), Button("2", id = "2"), Button("3", id = "3"),
        Button("+", variant = ButtonVariant.Warning, id = "plus"),
        Button("0", id = "0"), Button(".", id = "dot"),
        Button("=", variant = ButtonVariant.Primary, id = "equals"),
    )

    init {
        attach(Header(title = "Calculator"))
        attach(display)
        buttons.forEach { attach(it) }
    }
}

public fun main() {
    val console = Console.system()
    val app = CalculatorApp()

    val displayPanel = Panel(
        renderable = Align(app.display.render(), align = Justify.Right, style = Style(bgcolor = Color.Black, color = Color.White, bold = true)),
        title = Text("Calculator", style = Style(bold = true)),
        box = Box.ROUNDED,
        borderStyle = Style(color = Color.Cyan),
    )
    console.print(displayPanel)

    // 4x5 button grid laid out as rows of Columns. The full grid layout lands in Phase 8.5.
    val rows = app.buttons.chunked(4)
    for (row in rows) {
        console.print(Columns(row.map { btn -> Padded(btn.render(), Padding(0, 1, 0, 1)) }))
    }
}
