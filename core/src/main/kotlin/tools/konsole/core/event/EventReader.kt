package tools.konsole.core.event

import tools.konsole.core.Position
import tools.konsole.core.tty.Tty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Reads bytes from a [Tty], feeds them to an [AnsiInputParser], and publishes
 * parsed events to a [SharedFlow].
 *
 * One dedicated coroutine on [Dispatchers.IO] runs the blocking read loop. A
 * SIGWINCH handler emits resize events. Cursor-position queries route through
 * a dedicated channel so the inquirer awaits the next response, not whatever
 * key happens to be next in the stream.
 *
 * Lifetime is bound to the owning [tools.konsole.core.Terminal]; calling
 * [shutdown] cancels the read coroutine and unblocks the reader.
 */
internal class EventReader(private val tty: Tty) {

    // Daemon dispatcher: tty.read() is a blocking native call that Kotlin
    // can't interrupt via coroutine cancellation. If this thread were
    // non-daemon, the JVM would refuse to exit on App shutdown until the user
    // pressed a key to release the read. Daemon threads don't block exit, so
    // the process terminates as soon as runBlocking returns even with read()
    // still in flight.
    private val readerDispatcher: ExecutorCoroutineDispatcher =
        java.util.concurrent.Executors
            .newSingleThreadExecutor { r ->
                Thread(r, "konsole-event-reader").apply { isDaemon = true }
            }
            .asCoroutineDispatcher()

    private val scope: CoroutineScope = CoroutineScope(readerDispatcher + SupervisorJob() + Job())

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
        // SIGWINCH → Resize event. Registered reflectively against
        // sun.misc.Signal because the JVM has no public Signal API. We do
        // this here rather than in the Tty because SIGWINCH is process-global
        // and the event channel lives here.
        try {
            val sigClass = Class.forName("sun.misc.Signal")
            val handlerInterface = Class.forName("sun.misc.SignalHandler")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                handlerInterface.classLoader,
                arrayOf(handlerInterface),
            ) { _, method, _ ->
                if (method.name == "handle") {
                    val s = tty.size()
                    scope.launch { _events.emit(Event.Resize(s.columns, s.rows)) }
                }
                null
            }
            val signal = sigClass.getConstructor(String::class.java).newInstance("WINCH")
            sigClass.getMethod("handle", sigClass, handlerInterface).invoke(null, signal, proxy)
        } catch (_: Throwable) {
            // Non-fatal: resize events won't fire, but the app still runs.
        }

        scope.launch {
            try {
                while (!stopped) {
                    val b: Int = tty.read()
                    if (b == Tty.EOF) break
                    var ev = parser.advance(b)
                    // If the parser needs more bytes (lone Esc, partial CSI),
                    // give the kernel buffer a brief window to deliver them.
                    // No follow-up after the timeout = the user typed a bare
                    // Esc (or the sequence is malformed); flush() forces the
                    // parser to resolve so the event isn't held until the
                    // *next* keypress. Without this, pressing Esc once does
                    // nothing and pressing it twice fires the first Esc.
                    while (ev == null && !stopped) {
                        val next: Int = tty.read(ESC_TIMEOUT_MS)
                        when (next) {
                            Tty.READ_EXPIRED -> {
                                ev = parser.flush()
                                break
                            }
                            Tty.EOF -> {
                                stopped = true
                                break
                            }
                            else -> ev = parser.advance(next)
                        }
                    }
                    if (ev != null) _events.emit(ev)
                }
            } finally {
                cursorPositions.close()
            }
        }
    }

    private companion object {
        // How long to wait for follow-up bytes after the parser signals it
        // needs more. 40 ms is comfortably above LAN/SSH latency for a single
        // CSI sequence's worth of bytes while still feeling instant for a
        // bare Esc keypress.
        private const val ESC_TIMEOUT_MS: Long = 40L
    }

    fun shutdown() {
        stopped = true
        try { tty.shutdown() } catch (_: Throwable) { /* ignore */ }
        scope.cancel()
    }
}
