package tools.konsole.core.event

import tools.konsole.core.Command
import tools.konsole.core.Ansi.CSI

/**
 * Enable terminal mouse-input reporting (X10/SGR/etc.).
 *
 * Emits multiple DEC private mode sets so the terminal will report mouse motion
 * and SGR-format coordinates regardless of which it supports.
 */
public data object EnableMouseCapture : Command {
    override fun writeAnsi(out: Appendable) {
        // 1000 = report button press; 1002 = also drag; 1003 = also any motion;
        // 1015 = urxvt extended; 1006 = SGR (preferred — handles >223 columns).
        out.append(CSI).append("?1000h")
        out.append(CSI).append("?1002h")
        out.append(CSI).append("?1003h")
        out.append(CSI).append("?1015h")
        out.append(CSI).append("?1006h")
    }
}

public data object DisableMouseCapture : Command {
    override fun writeAnsi(out: Appendable) {
        // Reverse order of enable.
        out.append(CSI).append("?1006l")
        out.append(CSI).append("?1015l")
        out.append(CSI).append("?1003l")
        out.append(CSI).append("?1002l")
        out.append(CSI).append("?1000l")
    }
}

/** Tell the terminal to emit `CSI I` / `CSI O` on focus gain / loss. */
public data object EnableFocusChange : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1004h") }
}

public data object DisableFocusChange : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1004l") }
}

/** Enable bracketed paste so paste content is delivered as [Event.Paste]. */
public data object EnableBracketedPaste : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2004h") }
}

public data object DisableBracketedPaste : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2004l") }
}

/**
 * Push a new set of [KeyboardEnhancementFlags] onto the terminal's stack.
 * Only takes effect on terminals supporting the Kitty keyboard protocol.
 */
public data class PushKeyboardEnhancementFlags(val flags: KeyboardEnhancementFlags) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append(">").append(flags.bits.toString()).append('u')
    }
}

/** Pop the most recently pushed Kitty keyboard flags. */
public data object PopKeyboardEnhancementFlags : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("<u") }
}
