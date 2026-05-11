package tools.konsole.textual.pilot

import kotlinx.coroutines.delay
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyModifiers
import tools.konsole.textual.app.App
import tools.konsole.textual.dom.DOMNode
import tools.konsole.textual.dom.query
import tools.konsole.textual.dom.queryOne
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Click
import tools.konsole.textual.events.Key
import tools.konsole.textual.events.MouseButton
import tools.konsole.textual.events.MouseMove
import tools.konsole.textual.events.Paste
import tools.konsole.textual.events.Resize
import tools.konsole.textual.widget.Widget

/**
 * Synthetic-input test harness. Mirrors Python textual's `Pilot`.
 *
 * Wraps an [App] backed by a [HeadlessDriver]. Inject events with [press] /
 * [click] / [hover] / [type] / [paste], drain the pump with [pause], read
 * rendered output via [screenAsString].
 *
 * Usage:
 *
 * ```
 * val pilot = Pilot(MyApp())
 * pilot.press("ctrl+c")
 * pilot.click("Button#submit")
 * pilot.pause(100)
 * pilot.screenAsString() shouldContain "saved"
 * ```
 *
 * Lifecycle: callers must invoke [start] / [stop] to begin/end the harness
 * (avoiding `App.run()`'s blocking semantics). [use] does both around a block.
 */
public class Pilot<A : App>(public val app: A) {

    /** The headless driver capturing output and routing events. */
    public val driver: HeadlessDriver = app.driver as? HeadlessDriver
        ?: error("Pilot requires a HeadlessDriver — pass HeadlessDriver() to App's constructor")

    /** Start the app's pump + driver (non-blocking). */
    public fun start() {
        app.start()
        driver.startApplicationMode()
    }

    /** Stop the app and driver. */
    public fun stop() {
        driver.stopApplicationMode()
        app.stop()
    }

    public suspend inline fun use(block: (Pilot<A>) -> Unit) {
        start()
        try { block(this) } finally { stop() }
    }

    /**
     * Send one or more key presses. Each [keys] entry is a textual key spec:
     * - single char: `"a"`, `"x"`, `"q"`
     * - special: `"escape"`, `"enter"`, `"tab"`, `"backspace"`, `"space"`, `"up"`/`"down"`/`"left"`/`"right"`, `"home"`/`"end"`, `"pageup"`/`"pagedown"`, `"delete"`, `"insert"`, `"f1"`..`"f12"`
     * - modifiers: `"ctrl+c"`, `"shift+tab"`, `"alt+x"`, `"ctrl+shift+s"`
     */
    public suspend fun press(vararg keys: String) {
        for (k in keys) {
            val ev = parseKey(k)
            driver.send(ev)
        }
        pause(10)
    }

    /** Inject a single [text] character via [press]. */
    public suspend fun type(text: String) {
        for (ch in text) driver.send(Key(KeyCode.Char(ch)))
        pause(10)
    }

    /** Send a paste event (bracketed-paste payload). */
    public suspend fun paste(text: String) {
        driver.send(Paste(text))
        pause(10)
    }

    /** Click at the given [x],[y] cell coordinates. */
    public suspend fun clickAt(x: Int, y: Int, button: MouseButton = MouseButton.Left) {
        driver.send(Click(x, y, button))
        pause(10)
    }

    /**
     * Click on the first widget matching [selector]. Walks the screen's DOM,
     * finds the first match, and emits a [Click] at the widget's origin (0,0
     * since the compositor's full region-tracking lands in Phase 9.5).
     */
    public suspend fun click(selector: String, button: MouseButton = MouseButton.Left) {
        val node = findOne(selector) ?: return
        if (node is Widget) {
            // Phase 10: emit at (0,0) — compositor positions arrive in 9.5.
            driver.send(Click(0, 0, button))
            // Also deliver the click directly to the target widget for unit-test scenarios.
            node.post(Click(0, 0, button))
        }
        pause(10)
    }

    /** Hover at the given coordinates (emits a MouseMove). */
    public suspend fun hoverAt(x: Int, y: Int) {
        driver.send(MouseMove(x, y))
        pause(10)
    }

    /** Resize the headless terminal (emits a Resize event). */
    public suspend fun resize(columns: Int, rows: Int) {
        driver.send(Resize(columns, rows))
        pause(10)
    }

    /** Sleep [millis] to let the message pump drain pending events. */
    public suspend fun pause(millis: Long = 50) {
        delay(millis)
    }

    /** Locate the first node matching [selector] on the current screen, or app DOM. */
    public fun findOne(selector: String): DOMNode? {
        val screen = app.currentScreen
        return screen?.queryOne(selector) ?: app.queryOne(selector)
    }

    /** All nodes matching [selector]. */
    public fun findAll(selector: String): List<DOMNode> {
        val screen = app.currentScreen
        return (screen?.query(selector) ?: emptyList()) + app.query(selector)
    }

    /** Current rendered output buffer — the bytes the driver would have written to the terminal. */
    public fun screenAsString(): String = driver.output

    /** Reset the captured output buffer. */
    public fun clearScreen() { driver.clearOutput() }

    // ---- internals ----

    private fun parseKey(spec: String): Key {
        val parts = spec.lowercase().split("+")
        val keyPart = parts.last()
        var mods = KeyModifiers.NONE
        for (p in parts.dropLast(1)) {
            mods = when (p) {
                "ctrl", "control" -> mods + KeyModifiers.CONTROL
                "shift" -> mods + KeyModifiers.SHIFT
                "alt", "meta" -> mods + KeyModifiers.ALT
                else -> mods
            }
        }
        val code: KeyCode = when (keyPart) {
            "escape", "esc" -> KeyCode.Esc
            "enter", "return" -> KeyCode.Enter
            "tab" -> KeyCode.Tab
            "backspace" -> KeyCode.Backspace
            "space" -> KeyCode.Char(' ')
            "up" -> KeyCode.Up
            "down" -> KeyCode.Down
            "left" -> KeyCode.Left
            "right" -> KeyCode.Right
            "home" -> KeyCode.Home
            "end" -> KeyCode.End
            "pageup", "page_up" -> KeyCode.PageUp
            "pagedown", "page_down" -> KeyCode.PageDown
            "delete", "del" -> KeyCode.Delete
            "insert", "ins" -> KeyCode.Insert
            in setOf("f1","f2","f3","f4","f5","f6","f7","f8","f9","f10","f11","f12") ->
                KeyCode.F(keyPart.removePrefix("f").toInt())
            else -> if (keyPart.length == 1) KeyCode.Char(keyPart[0]) else KeyCode.Null
        }
        return Key(code, mods)
    }
}
