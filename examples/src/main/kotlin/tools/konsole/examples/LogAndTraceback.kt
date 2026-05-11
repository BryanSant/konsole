package tools.konsole.examples

import tools.konsole.rich.Console
import tools.konsole.rich.logging.KonsoleLogHandler
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Phase 5 demo — slf4j-style logging with rich-formatted output and a
 * rich-rendered traceback for a deliberately-thrown exception.
 *
 *   ./gradlew :examples:runExample -Pexample=LogAndTraceback
 */
public fun main() {
    val console = Console.system()
    val handler = KonsoleLogHandler(
        console = console,
        markup = true,
        richTracebacks = true,
        tracebacksExtraLines = 2,
    )
    val root: Logger = Logger.getLogger("")
    root.handlers.forEach { root.removeHandler(it) }
    root.addHandler(handler)
    root.level = Level.ALL

    val log = Logger.getLogger("demo")
    log.fine("debug-level message — hidden unless level is FINE+")
    log.info("[bold]starting up[/]")
    log.warning("config file not found, using defaults")
    log.severe("[red]critical failure[/]")

    try {
        explode()
    } catch (t: Throwable) {
        console.print(t)
    }
}

private fun explode() {
    val xs = listOf(1, 2, 3)
    @Suppress("UNUSED_VARIABLE")
    val item = xs[42]
}
