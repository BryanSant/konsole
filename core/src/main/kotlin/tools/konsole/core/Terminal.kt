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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jline.terminal.Attributes
import org.jline.terminal.TerminalBuilder
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Krossterm's primary entry point. Wraps a jline `Terminal`, exposing a
 * Kotlin-idiomatic API for cursor movement, styling, raw mode, alternate
 * screen, and event reading.
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
 * The underlying jline terminal is reachable via [underlying] for power users
 * needing capability strings, terminfo, or signal hooks not yet exposed here.
 */
public class Terminal internal constructor(
    private val jline: org.jline.terminal.Terminal,
) : AutoCloseable {

    /** The terminal's output writer. Pass to [execute] / [queue] / `terminal { }`. */
    public val out: Writer get() = jline.writer()

    /** Escape hatch — direct access to the underlying jline terminal. */
    public val underlying: org.jline.terminal.Terminal get() = jline

    /** Current terminal dimensions in cells. */
    public val size: Size
        get() {
            val s = jline.size
            return Size(s.columns, s.rows)
        }

    private val eventReader: EventReader by lazy { EventReader(jline) }

    private var rawAttributesSaved: Attributes? = null

    /**
     * Switch into raw mode (no echo, no line buffering, no signal generation
     * from Ctrl-C / Ctrl-Z) for the duration of [block]. The previous attributes
     * are restored on any exit, including via exception.
     */
    public inline fun <T> rawMode(block: () -> T): T {
        val saved = enterRawModeInternal()
        try {
            return block()
        } finally {
            exitRawModeInternal(saved)
        }
    }

    @PublishedApi
    internal fun enterRawModeInternal(): Attributes {
        val saved = jline.enterRawMode()
        fixupBlockingRawMode(jline)
        rawAttributesSaved = saved
        return saved
    }

    @PublishedApi
    internal fun exitRawModeInternal(saved: Attributes) {
        jline.attributes = saved
        rawAttributesSaved = null
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
        rawAttributesSaved?.let { jline.attributes = it }
        eventReader.shutdown()
        jline.close()
    }

    public companion object {
        /**
         * Force raw-mode termios to block on reads. After [org.jline.terminal.Terminal.enterRawMode],
         * JLine 4.1.0 leaves `VMIN=0, VTIME=1` (poll with 100ms timeout). The
         * kernel then returns 0 bytes after each timeout, which Java's
         * `FileInputStream.read()` translates into `-1` (EOF). That kills every
         * input pump as soon as the buffer drains.
         *
         * We override to `VMIN=1, VTIME=0` so `read()` actually blocks until
         * at least one byte is available — the normal "raw cbreak" termios.
         *
         * Call this on any [org.jline.terminal.Terminal] right after
         * [org.jline.terminal.Terminal.enterRawMode].
         */
        @JvmStatic
        public fun fixupBlockingRawMode(jline: org.jline.terminal.Terminal) {
            val attrs = jline.attributes
            attrs.setControlChar(Attributes.ControlChar.VMIN, 1)
            attrs.setControlChar(Attributes.ControlChar.VTIME, 0)
            jline.attributes = attrs
        }

        /**
         * Open the system terminal — the user's actual TTY. Suitable for
         * interactive programs.
         */
        @JvmStatic
        public fun system(): Terminal {
            // nativeSignals(false): JLine 4.1.0's AbstractUnixSysTerminal
            // unconditionally registers every value of the Terminal.Signal
            // enum, which includes INFO (BSD/macOS-only). On Linux the
            // registration returns null and ConcurrentHashMap.put throws NPE,
            // taking the whole terminal construction down. We don't need
            // JLine-managed signal handling; we wire WINCH ourselves in
            // EventReader. See JLine issue tracker for the upstream bug.
            //
            // graphemeCluster(false): TerminalBuilder.build() otherwise probes
            // the terminal via CSI ?2027$p + DA1 + (fallback) CSI 6n cursor-
            // position queries to decide whether to enable mode 2027 for
            // emoji clustering. If the probe's drain window (default 25ms) is
            // too short for the terminal's response, the leftover cursor
            // report bytes get parsed/echoed during App run or leak to the
            // shell at exit. konsole doesn't use JLine's grapheme cluster
            // mode (rich's Cells does its own width calculation), so we skip
            // the probe entirely.
            val jline = TerminalBuilder.builder()
                .system(true)
                .nativeSignals(false)
                .graphemeCluster(false)
                .build()
            return Terminal(jline)
        }

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
        ): Terminal {
            val jline = TerminalBuilder.builder()
                .dumb(true)
                .streams(input, output)
                .build()
            return Terminal(jline)
        }
    }
}
