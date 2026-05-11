package tools.konsole.textual.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.compositor.Compositor
import tools.konsole.textual.compositor.StripSerializer
import tools.konsole.textual.driver.Driver
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Click
import tools.konsole.textual.events.Event
import tools.konsole.textual.events.Key
import tools.konsole.textual.events.Mount
import tools.konsole.textual.events.MouseMove
import tools.konsole.textual.events.Resize
import tools.konsole.textual.events.Unmount
import tools.konsole.textual.dom.DOMNode
import tools.konsole.textual.message.Message
import tools.konsole.textual.screen.Screen
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Toast
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
     * Screen dimensions in cells. Updated by [Resize] events from the driver.
     * Defaults to 80x24 (which the [HeadlessDriver] uses for tests).
     */
    public var screenWidth: Int = 80
        private set
    public var screenHeight: Int = 24
        private set

    /** Frame compositor. Re-sized on each [Resize] event. */
    public val compositor: Compositor = Compositor(Region(0, 0, screenWidth, screenHeight))

    /** Toasts queued to be rendered on the TOAST layer. */
    private val activeToasts: MutableList<Toast> = mutableListOf()

    /** Animation tick rate. 100ms is sufficient for most spinners; faster eats cycles. */
    public open val tickIntervalMs: Long = 100L

    /** Set true whenever a widget calls refresh(); cleared after the next renderFrame. */
    @Volatile private var dirty: Boolean = true

    /**
     * Compose the initial widget set. Override to provide the App's content.
     * Returns a flat sequence — if you want a custom screen, push it after `run()` starts.
     */
    public open fun compose(): Sequence<Widget> = emptySequence()

    /** Mount the default screen from [compose] if nothing is on the screen stack yet. */
    public fun ensureMounted() {
        if (currentScreen == null) {
            val default = DefaultScreen(compose().toList())
            pushScreen(default)
        }
    }

    private var driverPumpStarted: Boolean = false
    /**
     * Override of [tools.konsole.textual.message.MessagePump.start] that also
     * starts driver-event pumping and mounts the default screen. Test harnesses
     * (Pilot) call this directly without going through [run].
     */
    override fun start() {
        super.start()
        ensureMounted()
        if (!driverPumpStarted) {
            pumpDriverEvents()
            driverPumpStarted = true
        }
    }

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

    /** Show a [Toast] notification. Auto-dismisses after [Toast.timeout] when set. */
    public fun notify(toast: Toast) {
        synchronized(activeToasts) { activeToasts += toast }
        toast.timeout?.let { ms ->
            rootScope.launch {
                delay(ms)
                toast.dismiss()
                synchronized(activeToasts) { activeToasts.remove(toast) }
                dirty = true
            }
        }
        dirty = true
    }

    /** Mark the screen as needing a fresh paint. Called by [Widget.refresh] indirectly. */
    public fun requestRefresh() { dirty = true }

    /**
     * Render one frame: collect visible widgets, lay them out on the [compositor],
     * serialise to ANSI, and push through the driver. Public so tests / `Pilot`
     * can force a paint without waiting for the tick loop.
     */
    public fun renderFrame() {
        ensureMounted()
        compositor.clear()
        val widgets = currentScreen?.compose()?.toList().orEmpty()
        if (widgets.isNotEmpty()) compositor.arrange(widgets, Compositor.BASE)
        // Layered overlays: toasts pin to the bottom-right by default.
        synchronized(activeToasts) {
            var y = screenHeight - 4
            for (t in activeToasts) {
                if (t.dismissed) continue
                val region = Region(x = screenWidth - 32, y = y, width = 30, height = 3)
                compositor.placeAt(t, region, Compositor.TOAST)
                y -= 4
                if (y < 0) break
            }
        }
        val strips = compositor.render()
        driver.write(StripSerializer.serialize(strips, originX = 0, originY = 0))
        driver.flush()
    }

    /**
     * Run the app. Blocks until [exit] is called.
     * Designed to be called from `fun main()`.
     */
    public open fun run() {
        runBlocking(supervisor) {
            start()
            driver.startApplicationMode()
            try {
                renderFrame()
                while (isActive && !exited) {
                    delay(tickIntervalMs)
                    if (dirty) {
                        renderFrame()
                        dirty = false
                    }
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
        when (event) {
            is Resize -> {
                screenWidth = event.columns
                screenHeight = event.rows
                // Resize compositor's viewport.
                // The Compositor's viewport is val, so swap it via a fresh instance.
                // (Phase 9.10 will make this in-place.)
                // For now: best-effort — call placeAt afresh on the next renderFrame().
                dirty = true
                post(event)
                currentScreen?.post(event)
            }
            is Click, is MouseMove -> {
                val (x, y) = when (event) {
                    is Click -> event.x to event.y
                    is MouseMove -> event.x to event.y
                    else -> 0 to 0
                }
                // Find the topmost widget at the pointer; deliver directly.
                val target = compositor.hitTest(x, y)
                if (target != null) target.post(event)
                else currentScreen?.post(event)
                dirty = true
            }
            is Key -> {
                currentScreen?.post(event)
                val match = bindings.match(event) ?: currentScreen?.bindings?.match(event)
                if (match != null && match.action == "quit") exit()
                dirty = true
            }
            else -> {
                currentScreen?.post(event)
                dirty = true
            }
        }
    }

    /** Internal: a no-op screen wrapper around [App.compose] output for the default mount path. */
    private class DefaultScreen(private val widgets: List<Widget>) : Screen() {
        override fun compose(): Sequence<Widget> = widgets.asSequence()
    }
}
