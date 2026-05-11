package tools.konsole.examples

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.live.Live
import tools.konsole.rich.spinner.SPINNERS
import tools.konsole.rich.spinner.Spinner

/**
 * Animate every spinner in rich's catalogue.
 *
 *   ./gradlew :examples:runExample -Pexample=Spinners
 *
 * Each row shows the spinner's current frame; the table is re-rendered
 * 20 times a second via [Live] so the spinners actually spin. Runs for
 * eight seconds, then exits.
 */
public fun main(): Unit = runBlocking {
    val console = Console.system()
    val nameWidth = SPINNERS.keys.maxOf { it.length }

    val display = Renderable { _, _ ->
        sequence {
            yield(Segment("All ${SPINNERS.size} spinners (rich's _spinners.py catalogue):",
                Style(bold = true, color = Color.Cyan)))
            yield(Segment.LINE)
            yield(Segment.LINE)
            for ((name, data) in SPINNERS) {
                val frame = Spinner(name).frame()
                yield(Segment(name.padEnd(nameWidth), Style(color = Color.Yellow)))
                yield(Segment("  "))
                yield(Segment(frame, Style(color = Color.Magenta, bold = true)))
                yield(Segment("   "))
                yield(Segment("interval=${data.interval}ms frames=${data.frames.size}",
                    Style(dim = true)))
                yield(Segment.LINE)
            }
        }
    }

    Live(display, console, refreshPerSecond = 20.0).use {
        delay(8000L)
    }
}
