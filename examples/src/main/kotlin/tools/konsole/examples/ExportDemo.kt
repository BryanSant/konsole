package tools.konsole.examples

import java.io.File
import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.table.Column
import tools.konsole.rich.table.Table

/**
 * Record some styled output, then export to plain text, HTML, and SVG.
 *
 *   ./gradlew :examples:runExample -Pexample=ExportDemo
 *
 * Produces three files in `build/exports/`:
 *  - export-demo.txt  plain-text snapshot of the rendered output
 *  - export-demo.html self-contained HTML page reproducing the styles
 *  - export-demo.svg  SVG terminal screenshot using SVG_EXPORT_THEME
 */
public fun main() {
    val console = Console(record = true, width = 80)

    console.print(Panel(
        renderable = Text("Konsole export demo — produces .txt / .html / .svg snapshots.", style = Style(color = Color.Cyan)),
        title = Text("Export", style = Style(bold = true)),
        box = Box.ROUNDED,
        borderStyle = Style(color = Color.Magenta),
    ))

    val table = Table(box = Box.HEAVY_HEAD, title = Text("Top results", style = Style(bold = true)))
    table.addColumn(Column(header = Text("Rank", style = Style(bold = true))))
    table.addColumn(Column(header = Text("Name", style = Style(bold = true))))
    table.addColumn(Column(header = Text("Score", style = Style(bold = true))))
    table.addRow(Text("1"), Text("Alice", style = Style(color = Color.Green)), Text("99", style = Style(bold = true)))
    table.addRow(Text("2"), Text("Bob", style = Style(color = Color.Yellow)), Text("87"))
    table.addRow(Text("3"), Text("Carol", style = Style(color = Color.Red)), Text("65"))
    console.print(table)

    val out = File("build/exports")
    out.mkdirs()
    val txtPath = File(out, "export-demo.txt").absolutePath
    val htmlPath = File(out, "export-demo.html").absolutePath
    val svgPath = File(out, "export-demo.svg").absolutePath
    console.saveText(txtPath)
    console.saveHtml(htmlPath, title = "Konsole export demo")
    console.saveSvg(svgPath, title = "Konsole export demo")

    // Now print to a real terminal so the user sees the success messages.
    val out2 = Console.system()
    out2.print("[bold]Exports written:[/]")
    out2.print("  text → $txtPath")
    out2.print("  html → $htmlPath")
    out2.print("   svg → $svgPath")
}
