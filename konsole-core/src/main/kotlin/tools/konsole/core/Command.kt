package tools.konsole.core

import java.io.Writer

/**
 * A terminal action that can be serialized as ANSI escape sequences.
 *
 * Every cursor move, style change, screen clear, etc. implements this interface.
 * Pure data — commands hold no I/O and can be queued, reordered, or replayed.
 *
 * Commands are emitted via [queue] (no flush) or [execute] (flushes immediately).
 *
 * ```
 * out.execute(MoveTo(0, 0), Hide, Print("loading…"))
 * ```
 *
 * Or via the DSL:
 *
 * ```
 * out.terminal {
 *   moveTo(0, 0); hide(); print("loading…")
 * }
 * ```
 *
 * Custom commands are supported via the SAM-conversion shortcut:
 *
 * ```
 * val resetSgr = Command { it.append("\u001B[0m") }
 * ```
 */
public fun interface Command {
    /** Append the ANSI representation of this command to [out]. */
    public fun writeAnsi(out: Appendable)
}

/** Append a sequence of commands to this writer without flushing. */
public fun Appendable.queue(vararg commands: Command): Appendable = apply {
    for (c in commands) c.writeAnsi(this)
}

/** Append commands and flush. */
public fun Writer.execute(vararg commands: Command): Writer = apply {
    queue(*commands); flush()
}

/**
 * Wrap [block] in `BeginSynchronizedUpdate` / `EndSynchronizedUpdate` so the terminal
 * presents the result atomically (no flicker / partial frames). Restores on any exit.
 */
public inline fun <T> Writer.synchronizedUpdate(block: (Writer) -> T): T {
    tools.konsole.core.terminal.BeginSynchronizedUpdate.writeAnsi(this)
    try {
        return block(this)
    } finally {
        tools.konsole.core.terminal.EndSynchronizedUpdate.writeAnsi(this)
        flush()
    }
}

/**
 * Switch into the alternate screen buffer for the duration of [block]. The
 * main screen is restored on any exit, including via exception.
 */
public inline fun <T> Writer.alternateScreen(block: (Writer) -> T): T {
    tools.konsole.core.terminal.EnterAlternateScreen.writeAnsi(this)
    flush()
    try {
        return block(this)
    } finally {
        tools.konsole.core.terminal.LeaveAlternateScreen.writeAnsi(this)
        flush()
    }
}
