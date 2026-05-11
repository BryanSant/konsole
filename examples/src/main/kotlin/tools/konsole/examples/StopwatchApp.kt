package tools.konsole.examples

import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Digits
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Horizontal
import tools.konsole.textual.widgets.Vertical

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
public class StopwatchApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val display = Digits("00:00.0", id = "display")
    private val startBtn = Button("Start", variant = ButtonVariant.Success, id = "start")
    private val stopBtn = Button("Stop", variant = ButtonVariant.Error, id = "stop")
    private val resetBtn = Button("Reset", id = "reset")

    @Volatile private var running: Boolean = false
    @Volatile private var elapsedTenths: Int = 0

    override val bindings = bindings(
        "q" to "quit",
        "ctrl+c" to "quit",
        "s" to "start",
        "p" to "stop",
        "r" to "reset",
    )

    @Suppress("unused") public fun action_start() { startTimer() }
    @Suppress("unused") public fun action_stop() { stopTimer() }
    @Suppress("unused") public fun action_reset() { resetTimer() }

    override val tickIntervalMs: Long = 100L  // tenth-of-second display granularity

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(
                Header(title = "Stopwatch"),
                display,
                Horizontal(startBtn, stopBtn, resetBtn),  // 3 buttons sharing a row
                Footer(this.bindings),
            ),
            heights = listOf(
                Scalar(1.0, LengthUnit.Cells),       // header
                Scalar(5.0, LengthUnit.Cells),       // Digits font is 5 rows tall
                Scalar(1.0, LengthUnit.Fraction),    // button row absorbs remaining space
                Scalar(1.0, LengthUnit.Cells),       // footer
            ),
        )
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

public fun main() {
    StopwatchApp().run()
}
