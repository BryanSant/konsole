package tools.konsole.rich.live

import tools.konsole.core.Hide
import tools.konsole.core.MoveToColumn
import tools.konsole.core.MoveUp
import tools.konsole.core.Show
import tools.konsole.core.execute
import tools.konsole.rich.Console
import tools.konsole.rich.Renderable
import tools.konsole.rich.layout.collectLines
import tools.konsole.core.terminal.Clear
import tools.konsole.core.terminal.ClearType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

/**
 * Live-updating display. Renders a [Renderable] in place, redrawing it on a fixed interval and
 * whenever [refresh] / [update] is called. Mirrors `rich.live.Live`.
 *
 * Lifecycle:
 *   ```
 *   Live(content, console).use { live ->
 *       live.start()
 *       // ... mutate state, call live.update(newRenderable) or just live.refresh()
 *   }
 *   ```
 *
 * On exit:
 *   - If [transient] is true, the rendered region is erased.
 *   - Otherwise the final frame is left on screen.
 *   - The cursor is restored to visible regardless.
 */
public class Live(
    renderable: Renderable,
    public val console: Console,
    public val refreshPerSecond: Double = 12.5,
    public val transient: Boolean = false,
    public val autoRefresh: Boolean = true,
    public val verticalOverflow: VerticalOverflow = VerticalOverflow.Ellipsis,
) : AutoCloseable {

    public enum class VerticalOverflow { Crop, Ellipsis, Visible }

    private val current: AtomicReference<Renderable> = AtomicReference(renderable)
    private val mutex = Any()
    private var renderedLines: Int = 0
    private var refreshJob: Job? = null
    private var scope: CoroutineScope? = null
    private var started: Boolean = false

    /** Replace the displayed [Renderable]. Triggers an immediate refresh if started. */
    public fun update(renderable: Renderable, refresh: Boolean = true) {
        current.set(renderable)
        if (refresh && started) refresh()
    }

    /** Force a redraw using the current [Renderable]. */
    public fun refresh() {
        synchronized(mutex) {
            if (!started) return
            drawFrame()
        }
    }

    /** Start the live display and the refresh coroutine (if [autoRefresh]). */
    public fun start() {
        if (started) return
        started = true
        // Hide cursor.
        console.terminal?.let { console.writer.execute(Hide) }
        // Initial draw.
        synchronized(mutex) { drawFrame() }
        if (autoRefresh) {
            val s = CoroutineScope(Dispatchers.IO)
            scope = s
            refreshJob = s.launch {
                val intervalMs = (1000.0 / refreshPerSecond).toLong().coerceAtLeast(16L)
                while (isActive) {
                    delay(intervalMs)
                    synchronized(mutex) { if (started) drawFrame() }
                }
            }
        }
    }

    /** Stop the refresh loop and clean up. */
    public fun stop() {
        if (!started) return
        started = false
        runBlocking { refreshJob?.cancelAndJoin() }
        refreshJob = null
        scope = null

        synchronized(mutex) {
            if (transient) {
                eraseRendered()
            }
        }

        // Show cursor again.
        if (console.terminal != null) console.writer.execute(Show)
    }

    override fun close() {
        stop()
    }

    /** Render the current renderable, overwriting the previous frame. */
    private fun drawFrame() {
        val renderable = current.get()
        val opts = console.defaultRenderOptions()
        val lines = collectLines(renderable.render(console, opts)).let { lines ->
            // Apply vertical-overflow if specified.
            val cap = console.height
            if (cap > 0 && lines.size > cap && verticalOverflow != VerticalOverflow.Visible) {
                lines.take(cap)
            } else lines
        }

        // Move back to the top of the previously rendered region and clear from cursor down.
        eraseRendered()

        val terminal = console.terminal
        val writer = console.writer
        val emit = {
            for ((i, line) in lines.withIndex()) {
                for (s in line.segments) writer.execute(tools.konsole.core.style.Print(s.text))
                // After each line except the last, emit \n
                if (i < lines.lastIndex) {
                    writer.write("\n")
                }
            }
            writer.flush()
        }
        if (terminal != null) terminal.synchronizedUpdate(emit) else emit()
        renderedLines = lines.size
    }

    private fun eraseRendered() {
        if (renderedLines == 0) return
        val writer = console.writer
        // Move to start of current line, then up to the top of our region.
        writer.execute(MoveToColumn(0))
        if (renderedLines > 1) writer.execute(MoveUp(renderedLines - 1))
        writer.execute(Clear(ClearType.FromCursorDown))
        renderedLines = 0
    }
}
