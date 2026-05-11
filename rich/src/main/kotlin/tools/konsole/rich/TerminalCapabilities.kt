package tools.konsole.rich

import tools.konsole.core.ColorSystem
import tools.konsole.core.Terminal

/**
 * Detect the terminal's color capability from a konsole [Terminal] (or `null` for non-TTY).
 *
 * Wraps [ColorSystem.detect] and supplies the `isTty` flag from the terminal type:
 * a dumb terminal is treated as non-TTY.
 */
public fun ColorSystem.Companion.detect(terminal: Terminal?): ColorSystem {
    val isTty = terminal != null && terminal.underlying.type != "dumb"
    return detect(isTty)
}
