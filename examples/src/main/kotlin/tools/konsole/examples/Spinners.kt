package tools.konsole.examples

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Columns
import tools.konsole.rich.spinner.SPINNERS
import tools.konsole.rich.spinner.Spinner

/**
 * Show every one of the 73 spinners from rich's catalogue.
 *
 *   ./gradlew :examples:runExample -Pexample=Spinners
 *
 * Each spinner is rendered with its name; the loop advances the monotonic
 * clock by a frame's interval so successive runs show different frames.
 */
public fun main(): Unit = runBlocking {
    val console = Console.system()
    console.print(Text("All ${SPINNERS.size} spinners (rich's _spinners.py catalogue):", style = Style(bold = true, color = Color.Cyan)))
    console.print()

    val nameWidth = SPINNERS.keys.maxOf { it.length }
    for ((name, data) in SPINNERS) {
        val spinner = Spinner(name)
        val frame = spinner.frame()
        val padded = name.padEnd(nameWidth)
        val line = Text()
        line.append(padded, Style(color = Color.Yellow))
        line.append("  ")
        line.append(frame, Style(color = Color.Magenta, bold = true))
        line.append("   ")
        line.append("interval=${data.interval}ms frames=${data.frames.size}", Style(dim = true))
        console.print(line)
    }
}
