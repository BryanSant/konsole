package tools.konsole.rich.status

import tools.konsole.rich.Console
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.live.Live
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.spinner.Spinner

/**
 * Displays a spinner with a status message until [stop] is called.
 *
 * Created via [Console.status] (recommended) or directly. Implements [AutoCloseable] so it
 * works with `use { }`.
 */
public class Status(
    public val console: Console,
    status: Renderable,
    public val spinnerName: String = "dots",
    public val spinnerStyle: Style? = null,
    public val speed: Double = 1.0,
    public val refreshPerSecond: Double = 12.5,
) : AutoCloseable {

    private var current: Renderable = status
    private val live: Live

    init {
        val style = spinnerStyle ?: console.theme["status.spinner"] ?: Style()
        val spinner = Spinner(spinnerName, text = current, style = style, speed = speed)
        live = Live(spinner, console, refreshPerSecond = refreshPerSecond, transient = true)
    }

    /** Update the displayed message; optionally swap spinner [name] or [style]. */
    public fun update(
        status: Renderable,
        spinner: String = this.spinnerName,
        spinnerStyle: Style? = this.spinnerStyle,
        speed: Double = this.speed,
    ) {
        current = status
        val style = spinnerStyle ?: console.theme["status.spinner"] ?: Style()
        live.update(Spinner(spinner, text = current, style = style, speed = speed))
    }

    /** Update with a markup string. */
    public fun update(status: String): Unit = update(Markup.parse(status))

    public fun start() {
        live.start()
    }

    public fun stop() {
        live.stop()
    }

    override fun close() {
        stop()
    }
}

/**
 * Convenience: open a [Status] context, run [block], and close.
 *
 * Usage:
 *   ```
 *   console.status("Loading…").use { status -> ... }
 *   ```
 */
public fun Console.status(
    message: String,
    spinner: String = "dots",
    spinnerStyle: Style? = null,
    speed: Double = 1.0,
    refreshPerSecond: Double = 12.5,
): Status {
    val s = Status(
        console = this,
        status = Markup.parse(message),
        spinnerName = spinner,
        spinnerStyle = spinnerStyle,
        speed = speed,
        refreshPerSecond = refreshPerSecond,
    )
    s.start()
    return s
}
