package tools.konsole.textual.driver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tools.konsole.core.InputMode
import tools.konsole.core.Terminal
import tools.konsole.core.event.Event as CoreEvent
import tools.konsole.core.event.KeyEvent
import tools.konsole.core.terminal.EnterAlternateScreen
import tools.konsole.core.terminal.LeaveAlternateScreen
import tools.konsole.textual.events.AppBlur
import tools.konsole.textual.events.AppFocus
import tools.konsole.textual.events.Click
import tools.konsole.textual.events.Event as TextualEvent
import tools.konsole.textual.events.Key
import tools.konsole.textual.events.MouseMove
import tools.konsole.textual.events.Paste
import tools.konsole.textual.events.Resize

/**
 * Real-terminal driver backed by [tools.konsole.core.Terminal] (JLine FFM).
 * Mirrors Python textual's `LinuxDriver` but the implementation is platform-
 * agnostic: JLine's FFM provider abstracts raw mode, SIGWINCH (Console API on
 * Windows), and the byte streams. Works on Linux, macOS, and Windows Terminal
 * 1.25+ (which supports the kitty keyboard protocol). Web driver is v2.
 *
 * On [startApplicationMode]:
 *  1. enter raw mode
 *  2. enter alternate screen
 *  3. send `InputMode.EnableAll` (bracketed paste, focus events, SGR mouse, kitty kbd, in-band resize)
 *
 * Input events from [Terminal.events] are translated to textual-layer [TextualEvent]s
 * and re-emitted on a [SharedFlow]. Output [write] calls go through the terminal's
 * writer; concurrent writes are serialised by the terminal.
 */
public class TerminalDriver(
    public val terminal: Terminal,
    private val enableKittyKeyboard: Boolean = true,
    private val enableMouseMotion: Boolean = false,
) : Driver {

    private val eventFlow: MutableSharedFlow<TextualEvent> = MutableSharedFlow(extraBufferCapacity = 256)
    override val events: SharedFlow<TextualEvent> get() = eventFlow.asSharedFlow()

    private var started: Boolean = false
    private var rawSaved: org.jline.terminal.Attributes? = null
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var pumpJob: Job? = null

    override fun startApplicationMode() {
        if (started) return
        started = true
        rawSaved = terminal.underlying.enterRawMode()
        // JLine's enterRawMode leaves VMIN=0/VTIME=1, which makes FileInputStream
        // return -1 (EOF) after the 100ms timeout. Override to block on reads.
        Terminal.fixupBlockingRawMode(terminal.underlying)
        val sb0 = StringBuilder()
        EnterAlternateScreen.writeAnsi(sb0)
        terminal.out.append(sb0)
        val mode = InputMode.EnableAll(kitty = enableKittyKeyboard, mouseMotion = enableMouseMotion)
        val sb = StringBuilder()
        mode.writeAnsi(sb)
        terminal.out.append(sb)
        terminal.out.flush()

        // Pump core events → textual events on the dedicated IO scope.
        pumpJob = terminal.events()
            .onEach { core ->
                val mapped = translate(core) ?: return@onEach
                eventFlow.tryEmit(mapped)
            }
            .launchIn(ioScope)
    }

    override fun stopApplicationMode() {
        if (!started) return
        pumpJob?.cancel()
        val disable = InputMode.DisableAll(kitty = enableKittyKeyboard, mouseMotion = enableMouseMotion)
        val sb = StringBuilder()
        disable.writeAnsi(sb)
        LeaveAlternateScreen.writeAnsi(sb)
        terminal.out.append(sb)
        terminal.out.flush()
        // Briefly drain any pending input bytes before restoring cooked-mode
        // termios. Terminals (and JLine itself, on close) may have queued up
        // responses to mode queries — most commonly a CSI cursor-position
        // report — that would otherwise leak to the shell prompt and either
        // print as `^[[…R` or get echoed character-by-character because the
        // shell receives them under ICANON+ECHO.
        try {
            val reader = terminal.underlying.reader()
            val deadlineMs = System.currentTimeMillis() + 50L
            while (System.currentTimeMillis() < deadlineMs) {
                val timeLeft = deadlineMs - System.currentTimeMillis()
                if (timeLeft <= 0) break
                val b = reader.read(timeLeft)
                if (b < 0) break  // EOF or timeout
            }
        } catch (_: Throwable) {
            // best-effort drain; never block shutdown
        }
        rawSaved?.let { terminal.underlying.attributes = it }
        started = false
    }

    override fun suspendApplicationMode() {
        if (!started) return
        val sb = StringBuilder()
        LeaveAlternateScreen.writeAnsi(sb)
        terminal.out.append(sb)
        terminal.out.flush()
    }

    override fun resumeApplicationMode() {
        if (!started) return
        val sb = StringBuilder()
        EnterAlternateScreen.writeAnsi(sb)
        terminal.out.append(sb)
        terminal.out.flush()
    }

    override fun write(data: String) {
        terminal.out.append(data)
    }

    override fun flush() {
        terminal.out.flush()
    }

    override fun disableInput() { /* core-side EventReader keeps running; we just filter */ }

    override fun enableInput() { /* see disableInput */ }

    private fun translate(core: CoreEvent): TextualEvent? = when (core) {
        is CoreEvent.Key -> Key(code = core.event.code, modifiers = core.event.modifiers)
        is CoreEvent.Mouse -> Click(core.event.column, core.event.row)
        is CoreEvent.Resize -> Resize(core.columns, core.rows)
        CoreEvent.FocusGained -> AppFocus()
        CoreEvent.FocusLost -> AppBlur()
        is CoreEvent.Paste -> Paste(core.text)
        else -> null  // Color reports, ModeReports, etc. are application-managed
    }
}
