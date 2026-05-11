package tools.konsole.textual.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.driver.Driver
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Event
import tools.konsole.textual.events.Key
import tools.konsole.textual.events.Mount
import tools.konsole.textual.events.Resize
import tools.konsole.textual.events.Unmount
import tools.konsole.textual.dom.DOMNode
import tools.konsole.textual.message.Message
import tools.konsole.textual.screen.Screen
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.worker.WorkerManager

/**
 * Top of the textual hierarchy. Mirrors Python textual's `App`.
 *
 * Subclass to define your application:
 *
 * ```
 * class MyApp : App() {
 *     override fun compose(): Sequence<Widget> = sequence { yield(Hello()) }
 * }
 *
 * MyApp().run()
 * ```
 *
 * Lifecycle (run()):
 *  1. Pick a [Driver] (LinuxDriver by default, [HeadlessDriver] if explicit).
 *  2. `driver.startApplicationMode()`.
 *  3. Mount the initial Screen (built from [compose]).
 *  4. Pump driver events into the root [MessagePump].
 *  5. Block until [exit] is called.
 *  6. `driver.stopApplicationMode()`.
 *
 * Threading model:
 *  - Root scope rooted at [run] via [runBlocking].
 *  - SupervisorJob so one widget crash doesn't take down the app.
 *  - Message pump consumer on [Dispatchers.Default].
 *  - Input/output IO on [Dispatchers.IO].limitedParallelism(1).
 */
public abstract class App(
    public val driver: Driver = HeadlessDriver(),
) : DOMNode() {

    private val supervisor = SupervisorJob()
    private val rootScope = CoroutineScope(supervisor + Dispatchers.Default)

    /** Worker manager bound to this app's root scope. */
    public val workers: WorkerManager = WorkerManager(rootScope)

    /** Bindings declared on the App (typically `q` → `quit`, etc). */
    public open val bindings: BindingsMap = BindingsMap()

    /** Screen stack. Top of stack receives input. */
    private val _screens: ArrayDeque<Screen> = ArrayDeque()
    public val screens: List<Screen> get() = _screens.toList()
    public val currentScreen: Screen? get() = _screens.firstOrNull()

    @Volatile private var exited = false

    /**
     * Compose the initial widget set. Override to provide the App's content.
     * Returns a flat sequence — if you want a custom screen, push it after `run()` starts.
     */
    public open fun compose(): Sequence<Widget> = emptySequence()

    /** Push [screen] onto the stack. */
    public fun pushScreen(screen: Screen) {
        _screens.addFirst(screen)
        attach(screen)
        screen.post(Mount())
    }

    /** Pop the top screen. */
    public fun popScreen(): Screen? {
        val s = _screens.removeFirstOrNull()
        if (s != null) { s.post(Unmount()); detach(s) }
        return s
    }

    /** Switch the top screen for [screen] (pop + push in one motion). */
    public fun switchScreen(screen: Screen) {
        popScreen()
        pushScreen(screen)
    }

    /** Notify the App to terminate after the current event drains. */
    public fun exit() { exited = true }

    /**
     * Run the app. Blocks until [exit] is called.
     * Designed to be called from `fun main()`.
     */
    public open fun run() {
        runBlocking(supervisor) {
            start()
            driver.startApplicationMode()
            try {
                // Initial mount of compose() into a default Screen if none pushed.
                if (currentScreen == null) {
                    val default = DefaultScreen(compose().toList())
                    pushScreen(default)
                }
                pumpDriverEvents()
                // Block until exit() is called
                while (!exited) {
                    kotlinx.coroutines.delay(50)
                }
            } finally {
                driver.stopApplicationMode()
                stop()
                supervisor.cancel()
            }
        }
    }

    private fun pumpDriverEvents() {
        driver.events
            .onEach { e ->
                handleEvent(e)
            }
            .launchIn(rootScope)
    }

    private fun handleEvent(event: Event) {
        // Routing: keys/clicks/etc. go to the current screen; resize goes to app + screen
        if (event is Resize) post(event)
        currentScreen?.post(event)
        if (event is Key) {
            // App-level bindings take precedence over screen widget bindings.
            val match = bindings.match(event) ?: currentScreen?.bindings?.match(event)
            if (match != null && match.action == "quit") exit()
        }
    }

    /** Internal: a no-op screen wrapper around [App.compose] output for the default mount path. */
    private class DefaultScreen(private val widgets: List<Widget>) : Screen() {
        override fun compose(): Sequence<Widget> = widgets.asSequence()
    }
}
