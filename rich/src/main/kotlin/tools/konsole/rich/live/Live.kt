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
    // Snapshot of the last frame's rendered rows, used to (a) overwrite
    // rows in place rather than wiping the whole region up-front (no
    // flicker on terminals without BSU support) and (b) skip emitting
    // rows whose content hasn't changed since the previous frame.
    private var prevRows: List<tools.konsole.rich.layout.CollectedLine> = emptyList()
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

    /**
     * Run [block] with the live display started, stopping it on exit
     * (including via exception). Shadows the stdlib `AutoCloseable.use`
     * extension so callers don't have to remember to call `start()`
     * inside the block — without `start()` the display is never painted
     * and every `refresh()` / `update()` becomes a no-op.
     */
    public inline fun <R> use(block: (Live) -> R): R {
        start()
        try {
            return block(this)
        } finally {
            stop()
        }
    }

    /**
     * Render the current renderable, overwriting the previous frame in place.
     *
     * The naive pattern is "Clear(FromCursorDown) then re-emit every row" —
     * but that leaves the region briefly empty between the clear and the
     * writes, which terminals without BSU support render as a visible
     * flash. Instead we move to the top of the prior region and overwrite
     * each row, using Clear(UntilNewLine) to wipe just that row's tail. If
     * the new frame has fewer rows than the previous one, the extra rows
     * are erased the same way (write empty + clear-to-EOL). Rows whose
     * segments are byte-for-byte identical to the previous frame's are
     * skipped — the cursor just advances over them with `\n`, no rewrite.
     */
    private fun drawFrame() {
        val renderable = current.get()
        val opts = console.defaultRenderOptions()
        val newRows = collectLines(renderable.render(console, opts)).let { lines ->
            val cap = console.height
            if (cap > 0 && lines.size > cap && verticalOverflow != VerticalOverflow.Visible) {
                lines.take(cap)
            } else lines
        }

        val terminal = console.terminal
        val writer = console.writer
        val emit = {
            // Park at the top of the previous region (col 0, first row).
            if (renderedLines > 0) {
                writer.execute(MoveToColumn(0))
                if (renderedLines > 1) writer.execute(MoveUp(renderedLines - 1))
            }

            val total = maxOf(newRows.size, prevRows.size)
            for (i in 0 until total) {
                val isLast = i == total - 1
                val newRow = newRows.getOrNull(i)
                val oldRow = prevRows.getOrNull(i)
                val unchanged = newRow != null && oldRow == newRow

                if (!unchanged) {
                    // Overwrite this row: park at col 0, write segments,
                    // wipe whatever was on the rest of the row.
                    writer.execute(MoveToColumn(0))
                    if (newRow != null) {
                        for (s in newRow.segments) writer.execute(tools.konsole.core.style.Print(s.text))
                    }
                    writer.execute(Clear(ClearType.UntilNewLine))
                }
                if (!isLast) writer.write("\n")
            }

            // If the new frame shrank, walk the cursor back up to row
            // (newRows.size - 1) so subsequent moves treat that as the
            // bottom. The trailing rows have already been blanked.
            if (newRows.size < prevRows.size) {
                writer.execute(MoveToColumn(0))
                val up = prevRows.size - newRows.size
                if (up > 0) writer.execute(MoveUp(up))
            }
            writer.flush()
        }
        if (terminal != null) terminal.synchronizedUpdate(emit) else emit()
        renderedLines = newRows.size
        prevRows = newRows
    }

    private fun eraseRendered() {
        if (renderedLines == 0) return
        val writer = console.writer
        // Move to start of current line, then up to the top of our region.
        writer.execute(MoveToColumn(0))
        if (renderedLines > 1) writer.execute(MoveUp(renderedLines - 1))
        writer.execute(Clear(ClearType.FromCursorDown))
        renderedLines = 0
        prevRows = emptyList()
    }
}
