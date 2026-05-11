package tools.konsole.examples

import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Padded
import tools.konsole.rich.layout.Padding
import tools.konsole.rich.panel.Panel
import tools.konsole.textual.app.App
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.pilot.Pilot
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Label

/**
 * Phase 11 textual demo — the canonical Stopwatch app from the textual tutorial,
 * adapted to konsole's Phase 9 widget set.
 *
 *   ./gradlew :examples:runExample -Pexample=StopwatchApp
 *
 * The textual original wires Start/Stop/Reset buttons to a Digits widget showing
 * elapsed time. Konsole's render loop hasn't fully landed (Phase 9.5+), so this
 * demo constructs the widget tree and renders one frame to the console — proving
 * the App/Widget/Compositor wiring compiles end-to-end.
 */
public class StopwatchApp : App(HeadlessDriver()) {

    public val display: Label = Label("00:00.00", id = "display")
    public val start: Button = Button("Start", variant = ButtonVariant.Success, id = "start")
    public val stop: Button = Button("Stop", variant = ButtonVariant.Error, id = "stop")
    public val reset: Button = Button("Reset", id = "reset")

    init {
        attach(Header(title = "Stopwatch"))
        attach(display)
        attach(start)
        attach(stop)
        attach(reset)
    }
}

public fun main() {
    val console = Console.system()
    val app = StopwatchApp()

    // Render each widget once to demonstrate the wiring; the full event loop arrives in Phase 9.5.
    console.print(Panel(app.display.render(), title = tools.konsole.rich.Text("Display"), box = Box.ROUNDED))
    console.print(Padded(app.start.render(), Padding(0, 2, 0, 2)))
    console.print(Padded(app.stop.render(), Padding(0, 2, 0, 2)))
    console.print(Padded(app.reset.render(), Padding(0, 2, 0, 2)))

    // Drive the buttons via the Pilot harness to prove message delivery works.
    var startPressed = false
    app.start.start()
    app.start.onMessage<Button.Pressed> { startPressed = true }
    runBlocking {
        val pilot = Pilot(app)
        pilot.use { p ->
            app.start.press()
            p.pause(50)
        }
    }
    app.start.stop()
    console.print("Start.press() delivered: $startPressed")
}
