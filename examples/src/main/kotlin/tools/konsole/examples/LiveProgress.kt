package tools.konsole.examples

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.rich.progress.Progress
import tools.konsole.rich.progress.columns.BarColumn
import tools.konsole.rich.progress.columns.SpinnerColumn
import tools.konsole.rich.progress.columns.TaskProgressColumn
import tools.konsole.rich.progress.columns.TextColumn
import tools.konsole.rich.progress.columns.TimeElapsedColumn
import tools.konsole.rich.progress.columns.TimeRemainingColumn

/**
 * Phase 4 demo — concurrent progress bars with custom columns.
 *
 *   ./gradlew :examples:runExample -Pexample=LiveProgress
 *
 * Note: rich Progress requires a terminal supporting cursor movement. Running
 * under Gradle's piped stdout shows the final rendered frames only.
 */
public fun main(): Unit = runBlocking {
    val console = Console.system()
    val progress = Progress(
        console = console,
        columns = listOf(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            TaskProgressColumn(),
            TimeElapsedColumn(),
            TimeRemainingColumn(),
        ),
    )

    progress.use {
        val downloading = progress.addTask("Downloading…", total = 100.0)
        val processing = progress.addTask("Processing…", total = 50.0)
        repeat(100) { i ->
            progress.advance(downloading, advance = 1.0)
            if (i % 2 == 0) progress.advance(processing, advance = 1.0)
            delay(40)
        }
    }
}
