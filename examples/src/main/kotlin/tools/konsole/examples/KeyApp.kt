package tools.konsole.examples

import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.events.Event as TextualEvent
import tools.konsole.textual.events.Key
import tools.konsole.textual.widget.Widget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Like KeyEcho but goes through the full App stack: systemDriver →
 * TerminalDriver event pump → App.handleEvent → bindings.match → exit().
 *
 * Each key event is also logged to stderr so we can see whether events
 * actually reach the App, whether bindings match, and whether action()
 * runs.
 *
 *   ./demo.sh KeyApp
 *
 * Press q to quit. ESC prints additional diagnostic info.
 */
public class KeyApp : App(systemDriver()) {

    private val display = StatusLabel()

    override val bindings = bindings(
        "q" to "quit",
        "escape" to "diagnose",
    )

    @Suppress("unused")
    public fun action_diagnose() {
        System.err.println("[KeyApp] action_diagnose() invoked; bindings size=${this.bindings.all().size}")
    }

    override fun action_quit() {
        System.err.println("[KeyApp] action_quit() called — exit() should fire next")
        super.action_quit()
        System.err.println("[KeyApp] after super.action_quit(): exited should now be true (run loop tick will catch it)")
    }

    override fun compose(): Sequence<Widget> = sequenceOf(display)

    init {
        // Tap the driver's event flow ourselves so we can trace whether
        // events make it past the driver layer.
        val tap: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        driver.events
            .onEach { e: TextualEvent -> System.err.println("[KeyApp] driver.events emitted: $e") }
            .launchIn(tap)
    }
}

private class StatusLabel : Widget() {
    override fun render(): Renderable {
        val t = Text()
        t.append("KeyApp running. ", Style(bold = true))
        t.append("Press 'q' to quit, ESC to print diagnose info. Output goes to stderr.")
        return t
    }
}

public fun main() {
    System.err.println("[KeyApp] starting App.run() …")
    KeyApp().run()
    System.err.println("[KeyApp] App.run() returned cleanly")
}
