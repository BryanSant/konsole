package tools.konsole.examples

import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.core.style.Color

/**
 * Phase 2 demo — markup parser, styles, manual Text spans, color palette.
 *
 *   ./gradlew :examples:runExample -Pexample=MarkupTour
 */
public fun main() {
    val console = Console.system()

    console.print("[bold]Markup tour:[/]")
    console.print("  [bold red]bold red[/] [italic green]italic green[/] [underline]underline[/]")
    console.print("  [blink]blink[/] [strike]strike[/] [dim]dim[/] [reverse]reverse[/]")
    console.print()
    console.print("[bold]Color palette:[/]")
    console.print("  [red]red[/] [green]green[/] [yellow]yellow[/] [blue]blue[/] [magenta]magenta[/] [cyan]cyan[/]")
    console.print("  [rgb(255,128,0)]rgb truecolor[/] [#00ff80]hex truecolor[/] [ansi(196)]ansi 196[/]")
    console.print()

    val t = Text("manual ")
    t.append("spans", Style(bold = true, color = Color.Cyan))
    t.append(" with ")
    t.append("mixed", Style(italic = true, color = Color.Magenta))
    t.append(" styles")
    console.print(t)

    console.print()
    console.print("[bold]Escaping:[/]")
    console.print("  \\[brackets] render literally inside markup")
    console.print("  emoji shortcodes: :rocket: :heart: :thumbs_up: :sparkles:")
}
