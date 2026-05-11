package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Columns
import tools.konsole.rich.layout.Group
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.table.Column
import tools.konsole.rich.table.Table
import tools.konsole.rich.tree.Tree

/**
 * Phase 3 demo — Panel, Table, Tree, Columns laid out side-by-side.
 *
 *   ./gradlew :examples:runExample -Pexample=RenderableGallery
 */
public fun main() {
    val console = Console.system()

    val panel = Panel(
        renderable = Text("Inside a panel.\nMulti-line content.\nRounded corners.", style = Style(color = Color.Cyan)),
        title = Text("Panel", style = Style(bold = true)),
        box = Box.ROUNDED,
        borderStyle = Style(color = Color.Magenta),
    )

    val table = Table(title = Text("Table demo", style = Style(bold = true, color = Color.Yellow)), box = Box.HEAVY_HEAD)
    table.addColumn(Column(header = Text("Name", style = Style(bold = true))))
    table.addColumn(Column(header = Text("Score", style = Style(bold = true))))
    table.addColumn(Column(header = Text("Time", style = Style(bold = true))))
    table.addRow(Text("Alice"), Text("99", style = Style(color = Color.Green)), Text("00:42"))
    table.addRow(Text("Bob"), Text("87", style = Style(color = Color.Yellow)), Text("01:15"))
    table.addRow(Text("Carol"), Text("65", style = Style(color = Color.Red)), Text("02:03"))

    val tree = Tree(label = Text("project/", style = Style(bold = true, color = Color.Blue)))
    val src = tree.add(Text("src/"))
    src.add(Text("main.kt"))
    src.add(Text("util.kt"))
    val tests = tree.add(Text("tests/"))
    tests.add(Text("MainTest.kt"))
    tree.add(Text("build.gradle.kts"))
    tree.add(Text("README.md"))

    console.print(panel)
    console.print()
    console.print(table)
    console.print()
    console.print(tree)
    console.print()
    console.print(Columns(listOf(panel, tree)))
}
