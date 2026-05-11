package tools.konsole.core

import tools.konsole.core.Ansi.OSC
import tools.konsole.core.Ansi.ST

/**
 * Query the terminal for its current background color via OSC 11.
 *
 * Conformant terminals reply with `ESC]11;rgb:RRRR/GGGG/BBBBST` (or `BEL`
 * terminator). The response is parsed by the input parser and surfaced as
 * an [tools.konsole.core.event.Event] for theme detection (light vs dark).
 */
public data object QueryBackgroundColor : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("11;?").append(ST)
    }
}

/**
 * Query the terminal for its current foreground color via OSC 10.
 *
 * Reply format mirrors [QueryBackgroundColor].
 */
public data object QueryForegroundColor : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("10;?").append(ST)
    }
}

/**
 * Query an indexed entry of the terminal's 256-color palette via OSC 4.
 *
 * @param index palette index in `0..255`.
 *
 * Reply format: `ESC]4;<index>;rgb:RRRR/GGGG/BBBBST`.
 */
public data class QueryPaletteColor(val index: Int) : Command {
    init {
        require(index in 0..255) { "palette index must be 0..255, was $index" }
    }
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("4;").append(index.toString()).append(";?").append(ST)
    }
}
