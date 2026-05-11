package tools.konsole.core.event

import tools.konsole.core.Position
import tools.konsole.core.event.AnsiInputParser
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

    // Daemon dispatcher: jline.reader().read() is a blocking native call that
    // Kotlin can't interrupt via coroutine cancellation. If this thread were
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
        // SIGWINCH → Resize event. We register the handler directly via
        // sun.misc.Signal rather than jline.handle(), because JLine 4.1.0's
        // AbstractUnixSysTerminal blows up on Linux when initialising its
        // signal table (it includes BSD-only SIGINFO, which neither the FFM
        // nor sun.misc.Signal fallback can register; the null return value
        // is then put into a ConcurrentHashMap which rejects it with NPE).
        // We bypass the whole mechanism by passing nativeSignals(false) to
        // TerminalBuilder and installing our own handler here.
        //
        // The reflective sun.misc.Signal access mirrors what JLine's own
        // Signals fallback does on JVMs without a public Signal API.
        try {
            val sigClass = Class.forName("sun.misc.Signal")
            val handlerInterface = Class.forName("sun.misc.SignalHandler")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                handlerInterface.classLoader,
                arrayOf(handlerInterface),
            ) { _, method, _ ->
                if (method.name == "handle") {
                    val s = jline.size
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
