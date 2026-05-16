package tools.konsole.textual.driver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tools.konsole.core.InputMode
import tools.konsole.core.Terminal
import tools.konsole.core.event.Event as CoreEvent
import tools.konsole.core.Hide as HideCursor
import tools.konsole.core.terminal.EnterAlternateScreen
import tools.konsole.core.terminal.LeaveAlternateScreen
import tools.konsole.core.tty.RawModeHandle
import tools.konsole.textual.events.AppBlur
import tools.konsole.textual.events.AppFocus
import tools.konsole.textual.events.Click
import tools.konsole.textual.events.Event as TextualEvent
import tools.konsole.textual.events.Key
import tools.konsole.textual.events.MouseMove
import tools.konsole.textual.events.Paste
import tools.konsole.textual.events.Resize

/**
 * Real-terminal driver backed by [tools.konsole.core.Terminal]. Mirrors Python
 * textual's `LinuxDriver`; the underlying [tools.konsole.core.tty.Tty]
 * abstracts raw mode, sizing, and the byte streams across Linux and macOS
 * today. Windows driver is planned but not yet implemented.
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
    private var rawHandle: RawModeHandle? = null
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var pumpJob: Job? = null

    override fun startApplicationMode() {
        if (started) return
        started = true
        rawHandle = terminal.tty.enterRawMode()
        val sb0 = StringBuilder()
        EnterAlternateScreen.writeAnsi(sb0)
        HideCursor.writeAnsi(sb0)   // hide cursor for the duration of the App
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
        // We don't drain pending input here on purpose. EventReader's pump is
        // parked inside a blocking tty.read(); a drain on the main thread
        // would race with it. Restoring termios via the handle is enough.
        rawHandle?.close()
        rawHandle = null
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
