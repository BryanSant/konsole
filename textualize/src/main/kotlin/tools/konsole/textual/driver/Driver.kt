package tools.konsole.textual.driver

import kotlinx.coroutines.flow.Flow
import tools.konsole.textual.events.Event

/**
 * Abstracts terminal I/O for an [tools.konsole.textual.app.App]. Mirrors Python textual's
 * `Driver` ABC (`src/textual/driver.py`).
 *
 * A Driver owns the boundary between raw byte streams (input bytes from stdin /
 * mouse / kitty kbd protocol; output bytes to a writer) and the application's
 * structured event stream.
 *
 * Lifecycle:
 *  1. [startApplicationMode] — enter alternate screen, raw mode, enable bracketed paste / focus / mouse / kitty kbd.
 *  2. [events] — emit parsed [Event]s as the user interacts.
 *  3. [write] — push rendered output to the terminal (typically called by the App's writer thread).
 *  4. [stopApplicationMode] — restore terminal state (leave alt screen, disable enabled modes, exit raw mode).
 *
 * [suspendApplicationMode] / [resumeApplicationMode] are used when the App
 * temporarily drops out of the alt screen (e.g. spawning `$EDITOR`).
 */
public interface Driver {

    /** Stream of input events parsed from the underlying terminal. */
    public val events: Flow<Event>

    /** Initialise terminal state: alt screen, raw mode, mouse/paste/focus/kitty kbd. */
    public fun startApplicationMode()

    /** Restore terminal state, pairs with [startApplicationMode]. */
    public fun stopApplicationMode()

    /** Temporarily suspend application mode (e.g. before spawning a child program). */
    public fun suspendApplicationMode()

    /** Resume application mode after [suspendApplicationMode]. */
    public fun resumeApplicationMode()

    /** Write [data] (rendered terminal output) to the underlying stream. */
    public fun write(data: String)

    /** Flush any buffered output. */
    public fun flush()

    /** Disable input handling but keep [write] available. Used during modal dialogs. */
    public fun disableInput()

    /** Re-enable input after [disableInput]. */
    public fun enableInput()
}

/**
 * Build a [TerminalDriver] bound to the process's controlling terminal.
 * Use this as the [tools.konsole.textual.app.App] driver when running interactively.
 *
 * ```
 * class MyApp : App(systemDriver()) { … }
 * ```
 */
public fun systemDriver(
    enableKittyKeyboard: Boolean = true,
    enableMouseMotion: Boolean = false,
): Driver = TerminalDriver(
    terminal = tools.konsole.core.Terminal.system(),
    enableKittyKeyboard = enableKittyKeyboard,
    enableMouseMotion = enableMouseMotion,
)

