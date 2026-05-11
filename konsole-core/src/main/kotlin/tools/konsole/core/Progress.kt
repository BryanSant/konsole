package tools.konsole.core

import tools.konsole.core.Command
import tools.konsole.core.Ansi.OSC
import tools.konsole.core.Ansi.ST

/** The visual state of the terminal's progress indicator. */
public sealed interface ProgressState {
    /** Remove the progress indicator. */
    public data object Remove : ProgressState
    /** Normal progress bar at [percent] (0-100). */
    public data class Normal(val percent: Int) : ProgressState
    /** Error-state progress bar at [percent] (0-100). */
    public data class Error(val percent: Int) : ProgressState
    /** Indeterminate / busy spinner (no percentage). */
    public data object Indeterminate : ProgressState
    /** Paused progress bar at [percent] (0-100). */
    public data class Paused(val percent: Int) : ProgressState
}

/**
 * Set the terminal's taskbar/tab progress indicator via OSC 9;4.
 *
 * Supported by Windows Terminal, WezTerm, iTerm2, and ConEmu-based terminals.
 * Silently ignored by terminals that do not implement OSC 9;4.
 *
 * Example:
 * ```
 * out.execute(SetProgress(ProgressState.Normal(50)))  // 50% progress
 * out.execute(SetProgress(ProgressState.Indeterminate))
 * out.execute(ClearProgress)
 * ```
 */
public data class SetProgress(val state: ProgressState) : Command {
    init {
        when (state) {
            is ProgressState.Normal  -> require(state.percent in 0..100) { "percent must be 0-100, got ${state.percent}" }
            is ProgressState.Error   -> require(state.percent in 0..100) { "percent must be 0-100, got ${state.percent}" }
            is ProgressState.Paused  -> require(state.percent in 0..100) { "percent must be 0-100, got ${state.percent}" }
            else -> Unit
        }
    }

    override fun writeAnsi(out: Appendable) {
        val (code, percent) = when (state) {
            is ProgressState.Remove        -> 0 to 0
            is ProgressState.Normal        -> 1 to state.percent
            is ProgressState.Error         -> 2 to state.percent
            is ProgressState.Indeterminate -> 3 to 0
            is ProgressState.Paused        -> 4 to state.percent
        }
        out.append(OSC).append("9;4;").append(code.toString()).append(';').append(percent.toString()).append(ST)
    }
}

/** Remove the progress indicator. Equivalent to `SetProgress(ProgressState.Remove)`. */
public val ClearProgress: Command = SetProgress(ProgressState.Remove)
