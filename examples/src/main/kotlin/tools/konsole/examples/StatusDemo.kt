package tools.konsole.examples

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.status.status

/**
 * Show `Console.status()` — a spinner-with-message live indicator.
 *
 *   ./gradlew :examples:runExample -Pexample=StatusDemo
 *
 * Mirrors Python rich's `status.py` example: kicks off a "task" with a
 * spinner, periodically updates the message, then finishes.
 */
public fun main(): Unit = runBlocking {
    val console = Console.system()
    console.print("Launching deployment…")

    console.status("[bold green]Connecting to remote host[/]").use { status ->
        delay(800)
        status.update(Markup.parse("[bold yellow]Authenticating[/]"))
        delay(800)
        status.update(Markup.parse("[bold cyan]Transferring artifacts[/]"), spinner = "earth")
        delay(1200)
        status.update(Markup.parse("[bold magenta]Running migrations[/]"), spinner = "dots12")
        delay(1000)
        status.update(Markup.parse("[bold green]Restarting services[/]"), spinner = "bouncingBar")
        delay(1000)
    }

    console.print("[bold green]:rocket: deployment complete[/]")
}
