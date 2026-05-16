package tools.konsole.core

import tools.konsole.core.Ansi
import tools.konsole.core.Position
import tools.konsole.core.event.Event
import tools.konsole.core.event.EventReader
import tools.konsole.core.terminal.BeginSynchronizedUpdate
import tools.konsole.core.terminal.EndSynchronizedUpdate
import tools.konsole.core.terminal.EnterAlternateScreen
import tools.konsole.core.terminal.LeaveAlternateScreen
import tools.konsole.core.terminal.Size
import tools.konsole.core.tty.RawModeHandle
import tools.konsole.core.tty.Tty
import tools.konsole.core.tty.TtyFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Konsole's primary entry point. Wraps a [Tty], exposing a Kotlin-idiomatic
 * API for cursor movement, styling, raw mode, alternate screen, and event
 * reading.
 *
 * Resource lifetime is managed via [AutoCloseable]: prefer Kotlin's `use { }`
 * to guarantee clean teardown.
 *
 * ```
 * Terminal.system().use { t ->
 *   t.alternateScreen {
 *     t.out.execute(MoveTo(0, 0), Print("hello"))
 *   }
 * }
 * ```
 *
 * Power users needing low-level access (raw bytes, isatty per-stream, custom
 * protocol negotiation) can reach the [Tty] directly via [tty].
 */
public class Terminal internal constructor(
    /** Direct access to the underlying [Tty]. */
    public val tty: Tty,
) : AutoCloseable {

    /** The terminal's output writer. Pass to [execute] / [queue] / `terminal { }`. */
    public val out: Writer get() = tty.out

    /** Current terminal dimensions in cells. */
    public val size: Size get() = tty.size()

    private val eventReader: EventReader by lazy { EventReader(tty) }

    private var rawModeHandle: RawModeHandle? = null

    /**
     * Switch into raw mode (no echo, no line buffering, no signal generation
     * from Ctrl-C / Ctrl-Z) for the duration of [block]. The previous attributes
     * are restored on any exit, including via exception.
     */
    public inline fun <T> rawMode(block: () -> T): T {
        val handle = enterRawModeInternal()
        try {
            return block()
        } finally {
            exitRawModeInternal(handle)
        }
    }

    @PublishedApi
    internal fun enterRawModeInternal(): RawModeHandle {
        val handle = tty.enterRawMode()
        rawModeHandle = handle
        return handle
    }

    @PublishedApi
    internal fun exitRawModeInternal(handle: RawModeHandle) {
        handle.close()
        rawModeHandle = null
    }

    /**
     * Switch into the alternate screen buffer for the duration of [block]. The
     * main screen is restored on any exit, including via exception.
     */
    public inline fun <T> alternateScreen(block: () -> T): T {
        EnterAlternateScreen.writeAnsi(out)
        out.flush()
        try {
            return block()
        } finally {
            LeaveAlternateScreen.writeAnsi(out)
            out.flush()
        }
    }

    /**
     * Wrap [block] in `BeginSynchronizedUpdate` / `EndSynchronizedUpdate` so the
     * terminal presents the result atomically.
     */
    public inline fun <T> synchronizedUpdate(block: () -> T): T {
        BeginSynchronizedUpdate.writeAnsi(out)
        try {
            return block()
        } finally {
            EndSynchronizedUpdate.writeAnsi(out)
            out.flush()
        }
    }

    // ---- Event API ----

    /** Hot stream of terminal events. New collectors don't replay history. */
    public fun events(): Flow<Event> = eventReader.flow

    /** Suspend until the next event arrives. */
    public suspend fun readEvent(): Event = eventReader.flow.first()

    /** Suspend up to [timeout] for an event. Returns null on timeout. */
    public suspend fun pollEvent(timeout: Duration): Event? =
        withTimeoutOrNull(timeout) { readEvent() }

    /** Blocking variant for non-coroutine callers. */
    public fun readEventBlocking(): Event = runBlocking { readEvent() }

    /**
     * Query the current cursor position. Sends `ESC[6n` (DSR-CPR) and awaits
     * the terminal's response. Coordinates are 0-indexed.
     *
     * Must be called while raw mode is active and the terminal supports CPR;
     * times out after [timeout] (default 250 ms).
     */
    public suspend fun cursorPosition(timeout: Duration = 250.milliseconds): Position? {
        // Drain any stale responses that pre-date this query.
        while (eventReader.cursorPositionChannel.tryReceive().isSuccess) Unit
        out.append(Ansi.CSI).append("6n")
        out.flush()
        return withTimeoutOrNull(timeout) { eventReader.cursorPositionChannel.receive() }
    }

    override fun close() {
        rawModeHandle?.close()
        rawModeHandle = null
        eventReader.shutdown()
        tty.close()
    }

    public companion object {
        /**
         * Open the system terminal — the user's actual TTY. Suitable for
         * interactive programs. Falls back to a dumb impl when stdin/stdout
         * aren't attached to a real console.
         */
        @JvmStatic
        public fun system(): Terminal = Terminal(TtyFactory.system())

        /**
         * Construct a non-interactive terminal backed by the given streams,
         * suitable for tests and headless environments. Defaults to the
         * process's standard streams if none are supplied.
         */
        @JvmStatic
        @JvmOverloads
        public fun dumb(
            input: InputStream = System.`in`,
            output: OutputStream = System.out,
        ): Terminal = Terminal(TtyFactory.dumb(input, output))
    }
}
