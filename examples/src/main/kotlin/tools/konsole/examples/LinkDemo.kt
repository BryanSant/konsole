package tools.konsole.examples

import tools.konsole.core.EndLink
import tools.konsole.core.StartLink
import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text

/**
 * OSC 8 clickable terminal hyperlinks.
 *
 *   ./gradlew :examples:runExample -Pexample=LinkDemo
 *
 * Modern terminals (iTerm2, kitty, alacritty, ghostty, wezterm, vte-based
 * emulators) render OSC 8 sequences as clickable links. Terminals that
 * don't recognise the sequence display the visible label text and silently
 * drop the URL — the demo degrades gracefully.
 */
public fun main() {
    val console = Console.system()

    console.print(Text("Clickable terminal hyperlinks (OSC 8):", style = Style(bold = true, color = Color.Cyan)))
    console.print()

    // Option 1: via rich Style.link — markup-friendly
    val t = Text()
    t.append("This is ", Style(dim = true))
    t.append(
        "rich documentation",
        Style(color = Color.Blue, underline = true, link = "https://rich.readthedocs.io"),
    )
    t.append(", and this is ", Style(dim = true))
    t.append(
        "textual",
        Style(color = Color.Magenta, underline = true, link = "https://textual.textualize.io"),
    )
    t.append(", and this is the ", Style(dim = true))
    t.append(
        "JLine project",
        Style(color = Color.Green, underline = true, link = "https://github.com/jline/jline3"),
    )
    t.append(".", Style(dim = true))
    console.print(t)

    console.print()

    // Option 2: via raw OSC 8 commands — direct terminal control
    console.print(Text("Direct OSC 8 emission:", style = Style(bold = true)))
    val sb = StringBuilder()
    sb.append("Visit ")
    StartLink("https://kotlinlang.org").writeAnsi(sb)
    sb.append("kotlinlang.org")
    EndLink.writeAnsi(sb)
    sb.append(" for Kotlin docs.")
    // Embed pre-built ANSI into a text segment — console will pass it through.
    console.writer.write(sb.toString())
    console.writer.write("\n")
    console.writer.flush()
}
