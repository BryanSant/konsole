package tools.konsole.examples

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.message.Message
import tools.konsole.textual.pilot.Pilot
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Digits
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header

/**
 * The canonical Stopwatch demo — Digits clock with Start / Stop / Reset.
 *
 *   ./gradlew :examples:runExample -Pexample=StopwatchApp
 *
 * The Pilot script below drives the App through ~2 seconds of real
 * lifecycle: start the clock, watch it tick, click Stop, click Reset.
 * Output is captured via the [HeadlessDriver] and the final screen is
 * printed at the end so you see the rendered frames in your terminal.
 */
public class StopwatchApp : App(HeadlessDriver()) {

    private val display = Digits("00:00.0", id = "display")
    private val startBtn = Button("Start", variant = ButtonVariant.Success, id = "start")
    private val stopBtn = Button("Stop", variant = ButtonVariant.Error, id = "stop")
    private val resetBtn = Button("Reset", id = "reset")

    @Volatile private var running: Boolean = false
    @Volatile private var elapsedTenths: Int = 0

    override val bindings = bindings(
        "q" to "quit",
        "s" to "start",
        "p" to "stop",
        "r" to "reset",
    )

    override val tickIntervalMs: Long = 100L  // tenth-of-second display granularity

    override fun compose(): Sequence<Widget> = sequenceOf(
        Header(title = "Stopwatch"),
        display,
        startBtn,
        stopBtn,
        resetBtn,
        Footer(this.bindings),
    )

    init {
        startBtn.start()
        stopBtn.start()
        resetBtn.start()
        startBtn.onMessage<Button.Pressed> { startTimer() }
        stopBtn.onMessage<Button.Pressed> { stopTimer() }
        resetBtn.onMessage<Button.Pressed> { resetTimer() }
        // Drive the display from a single background ticker — fires every 100ms.
        setInterval(100L) {
            if (running) {
                elapsedTenths += 1
                refreshDisplay()
            }
        }
    }

    private fun startTimer() { running = true; refreshDisplay() }
    private fun stopTimer() { running = false; refreshDisplay() }
    private fun resetTimer() { running = false; elapsedTenths = 0; refreshDisplay() }

    private fun refreshDisplay() {
        val total = elapsedTenths
        val minutes = total / 600
        val seconds = (total / 10) % 60
        val tenths = total % 10
        display.update("%02d:%02d.%d".format(minutes, seconds, tenths))
        requestRefresh()
    }
}

public fun main(): Unit = runBlocking {
    val app = StopwatchApp()
    val pilot = Pilot(app)
    pilot.use { p ->
        // Initial frame
        p.pause(200)
        app.renderFrame()

        // Click Start, watch it tick for 800ms (~ 0.8 seconds elapsed)
        @Suppress("UNCHECKED_CAST")
        val start = pilot.findOne("#start") as? Button ?: error("missing #start")
        start.press()
        p.pause(800)
        app.renderFrame()

        // Stop the clock
        @Suppress("UNCHECKED_CAST")
        val stop = pilot.findOne("#stop") as? Button ?: error("missing #stop")
        stop.press()
        p.pause(200)
        app.renderFrame()

        // Reset
        @Suppress("UNCHECKED_CAST")
        val reset = pilot.findOne("#reset") as? Button ?: error("missing #reset")
        reset.press()
        p.pause(200)
        app.renderFrame()
    }

    // Print the captured driver output so you can see the rendered frames.
    val console = Console.system()
    console.print("[bold]Stopwatch demo finished.[/]")
    console.print("[dim]Captured driver output (last frame):[/]")
    console.writer.write(app.driver.toString())
    console.writer.write("\n")
    val driver = app.driver as HeadlessDriver
    console.writer.write(driver.output)
    console.writer.flush()
}
