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
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyModifiers
import tools.konsole.textual.events.Blur
import tools.konsole.textual.events.Click
import tools.konsole.textual.events.Event
import tools.konsole.textual.events.Focus
import tools.konsole.textual.events.Key
import tools.konsole.textual.events.Mount
import tools.konsole.textual.events.MouseDown
import tools.konsole.textual.events.MouseDrag
import tools.konsole.textual.events.MouseMove
import tools.konsole.textual.events.MouseUp
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
 *  1. Pick a [Driver] ([HeadlessDriver] by default for tests; pass [systemDriver] for real terminals).
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
     * Currently focused widget — receives keyboard input first. `null` means
     * no focus and keys flow straight to the current screen.
     */
    public var focused: Widget? = null
        private set

    /** Move keyboard focus to [widget], or clear it with `null`. */
    public fun setFocus(widget: Widget?) {
        if (focused === widget) return
        focused?.let { it.hasFocus = false; it.post(Blur()) }
        focused = widget
        widget?.let { it.hasFocus = true; it.post(Focus()) }
        dirty = true
    }

    /** Move focus to the next focusable widget in tab order (DOM pre-order). */
    public fun focusNext() {
        val focusable = collectFocusable()
        if (focusable.isEmpty()) return
        val idx = focusable.indexOf(focused)
        setFocus(focusable[if (idx < 0) 0 else (idx + 1) % focusable.size])
    }

    /** Move focus to the previous focusable widget in tab order. */
    public fun focusPrevious() {
        val focusable = collectFocusable()
        if (focusable.isEmpty()) return
        val idx = focusable.indexOf(focused)
        setFocus(focusable[if (idx < 0) focusable.lastIndex else (idx - 1 + focusable.size) % focusable.size])
    }

    private fun collectFocusable(): List<Widget> {
        val screen = currentScreen ?: return emptyList()
        return screen.walk().filterIsInstance<Widget>().filter { it.canFocus }.toList()
    }

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

    /**
     * Active TCSS stylesheet. Default [tools.konsole.textual.css.Stylesheet]
     * holds zero rules. Replace via [loadStylesheet] or assign directly;
     * widgets see the new styles on the next render.
     */
    @Volatile public var stylesheet: tools.konsole.textual.css.Stylesheet = tools.konsole.textual.css.Stylesheet()

    /** Load + replace the [stylesheet] from a `.tcss` file on disk. */
    public fun loadStylesheet(file: java.io.File) {
        stylesheet = tools.konsole.textual.css.Stylesheet.parse(file.readText(), file.name)
        invalidate()
        requestRefresh()
    }

    private var stylesheetWatcher: tools.konsole.textual.css.StylesheetWatcher? = null

    /**
     * Watch [file] for changes and hot-reload [stylesheet]. Mirrors textual's
     * `--dev` CSS auto-reload. The next frame after each change picks up the
     * new rules.
     *
     * Pair with [stopWatchingStylesheet] (or simply stop the app, which
     * cancels every scope).
     */
    public fun watchStylesheet(file: java.io.File) {
        stylesheetWatcher?.stop()
        loadStylesheet(file)
        val watcher = tools.konsole.textual.css.StylesheetWatcher(
            file = file,
            scope = rootScope,
            onChange = { content: String ->
                stylesheet = tools.konsole.textual.css.Stylesheet.parse(content, file.name)
                invalidate()
                requestRefresh()
            },
        )
        watcher.start()
        stylesheetWatcher = watcher
    }

    public fun stopWatchingStylesheet() {
        stylesheetWatcher?.stop()
        stylesheetWatcher = null
    }

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
    /** Auto-focus the first focusable widget when the app first mounts. Override to disable. */
    public open val autoFocusOnStart: Boolean get() = true

    override fun start() {
        super.start()
        ensureMounted()
        if (!driverPumpStarted) {
            pumpDriverEvents()
            driverPumpStarted = true
        }
        if (autoFocusOnStart && focused == null) {
            val firstFocusable = currentScreen?.walk()
                ?.filterIsInstance<Widget>()
                ?.firstOrNull { it.canFocus }
            if (firstFocusable != null) setFocus(firstFocusable)
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
     * Dispatch a named action. Walks focused → currentScreen → this App
     * looking for an `action_<name>` method via reflection; calls the first
     * match. Returns true if an action method was found and ran.
     *
     * Mirrors Python textual's `action_*` discovery — bindings declare
     * `"q"` → `"quit"`, and the App's `action_quit()` method runs.
     */
    public fun action(name: String, vararg args: Any?): Boolean {
        val targets = listOfNotNull(focused, currentScreen, this as DOMNode)
        for (target in targets) {
            val methodName = "action_$name"
            val method = try {
                target::class.java.declaredMethods.firstOrNull { it.name == methodName }
            } catch (_: Throwable) { null } ?: continue
            try {
                method.isAccessible = true
                method.invoke(target, *args)
                return true
            } catch (_: Throwable) { /* try next */ }
        }
        return false
    }

    /** Default built-in: `action_quit()` exits the app. */
    @Suppress("unused")
    public open fun action_quit() { exit() }

    /** Last widget the pointer was over — used to clear the previous hover when the pointer moves. */
    private var hoveredWidget: Widget? = null

    /** Widget that received the most recent MouseDown — Click fires on MouseUp over the same widget. */
    private var pressedWidget: Widget? = null
    private var pressedX: Int = 0
    private var pressedY: Int = 0

    private fun updateHover(target: Widget?) {
        if (hoveredWidget === target) return
        hoveredWidget?.isHovered = false
        target?.isHovered = true
        hoveredWidget = target
    }

    /**
     * Render one frame: collect visible widgets, lay them out on the [compositor],
     * serialise to ANSI, and push through the driver. Public so tests / `Pilot`
     * can force a paint without waiting for the tick loop.
     */
    /** Previous frame's strips — kept for dirty-region diffing. `null` = next paint is full. */
    @Volatile private var previousFrame: List<tools.konsole.rich.Strip>? = null

    /** Force the next [renderFrame] to repaint the entire screen (skip diffing). */
    public fun invalidate() { previousFrame = null }

    /**
     * Hook for subclasses to customise base-layer layout. Default behaviour
     * is the compositor's row-per-widget vertical stack — fine for most apps,
     * but demos with full-screen rendering or hand-rolled grids override this
     * to call [Compositor.placeAt] explicitly.
     *
     * Invoked after [Compositor.clear] and before toast/overlay placement, so
     * overrides should not clear the compositor themselves.
     */
    protected open fun arrangeBaseLayer(widgets: List<Widget>) {
        if (widgets.isNotEmpty()) compositor.arrange(widgets, Compositor.BASE)
    }

    public fun renderFrame() {
        ensureMounted()
        compositor.clear()
        val widgets = currentScreen?.compose()?.toList().orEmpty()
        arrangeBaseLayer(widgets)
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
        val previous = previousFrame
        val ansi = if (previous == null || previous.size != strips.size) {
            // First frame or terminal resized — full repaint.
            StripSerializer.serialize(strips, originX = 0, originY = 0, clearFirst = previous == null)
        } else {
            StripSerializer.serializeDiff(previous, strips, originX = 0, originY = 0)
        }
        if (ansi.isNotEmpty()) {
            driver.write(ansi)
            driver.flush()
        }
        previousFrame = strips
    }

    /**
     * Run the app. Blocks until [exit] is called.
     * Designed to be called from `fun main()`.
     *
     * Terminates the JVM via [kotlin.system.exitProcess] once the run loop
     * completes — terminal attributes have already been restored, and JLine's
     * blocking native `read()` on stdin (running on a daemon thread JLine
     * starts internally) can otherwise hold the process alive until the user
     * presses a key. Override [exitProcessOnRun] to disable for embedded use.
     */
    public open fun run() {
        // `runBlocking` is intentionally NOT a child of [supervisor]: the
        // finally block cancels supervisor (and its children — pump coroutines,
        // workers, etc.) and if runBlocking's own coroutine were a child of
        // supervisor it would cancel itself mid-cleanup and surface a noisy
        // JobCancellationException as runBlocking unwinds.
        runBlocking {
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
        if (exitProcessOnRun) kotlin.system.exitProcess(0)
    }

    /**
     * Whether [run] should terminate the JVM via [kotlin.system.exitProcess]
     * once the run loop completes. Default `true`, which gives standalone TUI
     * apps the curses-style "return to shell immediately on quit" behaviour.
     * Override to `false` if the App is embedded in a larger JVM process that
     * needs to continue running.
     */
    public open val exitProcessOnRun: Boolean get() = true

    private fun pumpDriverEvents() {
        driver.events
            .onEach { e -> handleEvent(e) }
            .launchIn(rootScope)
    }

    private suspend fun handleEvent(event: Event) {
        when (event) {
            is Resize -> {
                screenWidth = event.columns
                screenHeight = event.rows
                compositor.resize(Region(0, 0, screenWidth, screenHeight))
                invalidate()
                dirty = true
                post(event)
                currentScreen?.post(event)
            }
            is MouseDown -> {
                val target = compositor.hitTest(event.x, event.y)
                updateHover(target)
                pressedWidget = target
                pressedX = event.x; pressedY = event.y
                target?.isPressed = true
                target?.post(event) ?: currentScreen?.post(event)
                dirty = true
            }
            is MouseUp -> {
                val target = compositor.hitTest(event.x, event.y)
                val pressed = pressedWidget
                pressed?.isPressed = false
                // If MouseUp landed on the same widget as MouseDown, synthesise a Click.
                if (pressed != null && pressed === target) {
                    pressed.post(Click(event.x, event.y, event.button))
                }
                target?.post(event) ?: currentScreen?.post(event)
                pressedWidget = null
                dirty = true
            }
            is Click -> {
                // Direct Click event (e.g. injected by Pilot or single-event drivers).
                val target = compositor.hitTest(event.x, event.y)
                updateHover(target)
                target?.let {
                    it.isPressed = true
                    rootScope.launch {
                        delay(80)
                        it.isPressed = false
                        requestRefresh()
                    }
                    it.post(event)
                } ?: currentScreen?.post(event)
                dirty = true
            }
            is MouseMove -> {
                val target = compositor.hitTest(event.x, event.y)
                updateHover(target)
                // If a button is held, also fire MouseDrag.
                val pressed = pressedWidget
                if (pressed != null) {
                    pressed.post(MouseDrag(event.x, event.y, tools.konsole.textual.events.MouseButton.Left, pressedX, pressedY))
                }
                target?.post(event) ?: currentScreen?.post(event)
                dirty = true
            }
            is Key -> {
                // Tab / Shift+Tab navigate focus before bindings run.
                if (event.code == KeyCode.Tab && event.modifiers.bits == 0) {
                    focusNext()
                    dirty = true
                    return
                }
                if (event.code == KeyCode.BackTab) {
                    focusPrevious()
                    dirty = true
                    return
                }
                // Bindings: focused → screen → app, action_<name> dispatch first.
                val match = focused?.bindings?.match(event)
                    ?: currentScreen?.bindings?.match(event)
                    ?: bindings.match(event)
                if (match != null) {
                    if (!action(match.action)) {
                        // Built-in fallback for "quit".
                        if (match.action == "quit") exit()
                    }
                } else {
                    // Forward to focused widget first, synchronously, so order
                    // is preserved against binding-action dispatch. If the
                    // widget consumes the event (event.stop()), we stop.
                    val target = focused
                    if (target != null) {
                        target.onEvent(event)
                    } else {
                        currentScreen?.post(event)
                    }
                }
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
        init {
            // Attach each composed widget to the screen so DOM queries (#id, .class)
            // and Pilot.findOne can locate them.
            for (w in widgets) attach(w)
        }
        override fun compose(): Sequence<Widget> = widgets.asSequence()
    }
}
