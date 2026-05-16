package tools.konsole.rich

import tools.konsole.core.ColorSystem
import tools.konsole.core.Terminal
import tools.konsole.core.tty.Stream

/**
 * Detect the terminal's color capability from a konsole [Terminal] (or `null` for non-TTY).
 *
 * Wraps [ColorSystem.detect] and supplies the `isTty` flag from the terminal:
 * a dumb terminal (stdout not a TTY) is treated as non-TTY.
 */
public fun ColorSystem.Companion.detect(terminal: Terminal?): ColorSystem {
    val isTty = terminal != null && terminal.tty.isatty(Stream.Output)
    return detect(isTty)
}
