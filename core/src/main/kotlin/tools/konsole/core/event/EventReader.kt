package tools.konsole.core.event

import tools.konsole.core.Position
import tools.konsole.core.event.AnsiInputParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jline.terminal.Terminal as JlineTerminal

/**
 * Reads bytes from a jline terminal, feeds them to an [AnsiInputParser], and
 * publishes parsed events to a [SharedFlow].
 *
 * One dedicated coroutine on [Dispatchers.IO] runs the blocking read loop. A
 * SIGWINCH handler emits resize events. Cursor-position queries route through
 * a dedicated channel so the inquirer awaits the next response, not whatever
 * key happens to be next in the stream.
 *
 * Lifetime is bound to the owning [tools.konsole.core.Terminal]; calling
 * [shutdown] cancels the read coroutine and unblocks the jline reader.
 */
internal class EventReader(private val jline: JlineTerminal) {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + Job())

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    val flow: SharedFlow<Event> get() = _events.asSharedFlow()

    private val cursorPositions = Channel<Position>(capacity = 8)
    val cursorPositionChannel: Channel<Position> get() = cursorPositions

    private val parser = AnsiInputParser().apply {
        onCursorPosition = { column, row ->
            cursorPositions.trySend(Position.of(column, row))
        }
        // onKeyboardFlags intentionally left null — we only push, never query, in this iteration.
    }

    @Volatile
    private var stopped: Boolean = false

    init {
        // SIGWINCH → Resize event with new dimensions. jline polls Console API on Windows.
        jline.handle(JlineTerminal.Signal.WINCH) { _ ->
            val s = jline.size
            // trySendBlocking would suspend; emit can't be called synchronously. Hop to the scope.
            scope.launch { _events.emit(Event.Resize(s.columns, s.rows)) }
        }

        scope.launch {
            val reader = jline.reader()
            try {
                while (!stopped) {
                    val b: Int = try {
                        reader.read()       // blocks; returns -1 on EOF, throws InterruptedIOException on shutdown
                    } catch (_: InterruptedException) {
                        break
                    } catch (_: java.io.InterruptedIOException) {
                        break
                    }
                    if (b < 0) break
                    val ev = parser.advance(b) ?: continue
                    _events.emit(ev)
                }
            } finally {
                cursorPositions.close()
            }
        }
    }

    fun shutdown() {
        stopped = true
        try {
            jline.reader().shutdown()
        } catch (_: Throwable) { /* ignore */ }
        scope.cancel()
    }
}
