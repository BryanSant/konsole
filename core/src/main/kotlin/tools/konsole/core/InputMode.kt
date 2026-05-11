package tools.konsole.core

import tools.konsole.core.Ansi.CSI
import tools.konsole.core.event.KeyboardEnhancementFlags

/**
 * Standardised enable/disable commands for the optional input-protocol features
 * that konsole drivers (and textual's [Driver]-equivalent) negotiate at startup.
 *
 * Emit [enableAll] when the application launches and [disableAll] when it exits
 * — they pair up. Terminals that don't support a feature ignore its enable byte
 * sequence silently, so it's safe to send unconditionally.
 *
 * Individual `Enable*` / `Disable*` commands are exposed for finer control.
 */
public object InputMode {

    // ---- Bracketed paste (DECSET 2004) ----

    /** Enable bracketed-paste mode (`CSI ?2004h`). Pasted text is wrapped in `CSI 200~ … CSI 201~`. */
    public data object EnableBracketedPaste : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2004h") }
    }

    /** Disable bracketed-paste mode (`CSI ?2004l`). */
    public data object DisableBracketedPaste : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2004l") }
    }

    // ---- Focus reporting (DECSET 1004) ----

    /** Enable focus-in/focus-out events (`CSI ?1004h`). Events arrive as `CSI I` / `CSI O`. */
    public data object EnableFocusEvents : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1004h") }
    }

    /** Disable focus-in/focus-out events (`CSI ?1004l`). */
    public data object DisableFocusEvents : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1004l") }
    }

    // ---- SGR-encoded mouse (DECSET 1000 + 1006) ----

    /**
     * Enable mouse-button reporting with SGR extended-coordinate encoding
     * (`CSI ?1000h` then `CSI ?1006h`). Events arrive as `CSI < button;col;row [Mm]`
     * and are parsed by [tools.konsole.core.event.AnsiInputParser].
     */
    public data object EnableSgrMouse : Command {
        override fun writeAnsi(out: Appendable) {
            out.append(CSI).append("?1000h")
            out.append(CSI).append("?1006h")
        }
    }

    /** Disable SGR mouse reporting. */
    public data object DisableSgrMouse : Command {
        override fun writeAnsi(out: Appendable) {
            out.append(CSI).append("?1006l")
            out.append(CSI).append("?1000l")
        }
    }

    /**
     * Enable mouse motion reporting as well (`CSI ?1003h`). Pair with [EnableSgrMouse]
     * for hover-tracking applications.
     */
    public data object EnableMouseMotion : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1003h") }
    }

    /** Disable mouse motion reporting. */
    public data object DisableMouseMotion : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1003l") }
    }

    // ---- Kitty keyboard protocol (`CSI > flags u` / `CSI < u`) ----

    /**
     * Push kitty keyboard protocol flags on the terminal's flag stack.
     * Sequence: `CSI > <flags> u`. Default request asks for all enhancements.
     */
    public data class EnableKittyKeyboard(
        val flags: KeyboardEnhancementFlags = KeyboardEnhancementFlags.ALL,
    ) : Command {
        override fun writeAnsi(out: Appendable) {
            out.append(CSI).append(">").append(flags.bits.toString()).append("u")
        }
    }

    /**
     * Disable the kitty keyboard protocol comprehensively. Some terminals
     * (notably ghostty 1.x) don't reliably restore the default state from a
     * lone pop, so we both:
     *  - explicitly set the flag register to 0 via `CSI = 0 ; 1 u`
     *    (clears every enhancement bit unconditionally), and
     *  - pop one entry from the protocol's flag stack via `CSI < u`
     *    (undoes [EnableKittyKeyboard]'s push for terminals that obey the
     *    stack semantics).
     *
     * Terminals that don't understand one form ignore it.
     */
    public data object DisableKittyKeyboard : Command {
        override fun writeAnsi(out: Appendable) {
            out.append(CSI).append("=0;1u")
            out.append(CSI).append("<u")
        }
    }

    // ---- In-band window-size notifications (xterm mode 2048) ----

    /**
     * Enable in-band window-size notifications (`CSI ?2048h`). Resize is reported
     * via `CSI 48 ; rows ; cols ; pixH ; pixW t` instead of SIGWINCH — required
     * for terminals that don't propagate the signal (Web, multiplexers).
     */
    public data object EnableInBandResize : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2048h") }
    }

    /** Disable in-band window-size notifications. */
    public data object DisableInBandResize : Command {
        override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2048l") }
    }

    // ---- Composite ----

    /**
     * Composite command emitting every "enable" sequence textual / konsole-textual
     * drivers want at startup: bracketed paste, focus events, SGR mouse, kitty
     * keyboard, in-band resize. Terminals that don't support a feature ignore the
     * unrecognised bytes.
     */
    public data class EnableAll(
        val kitty: Boolean = true,
        val mouseMotion: Boolean = false,
    ) : Command {
        override fun writeAnsi(out: Appendable) {
            EnableBracketedPaste.writeAnsi(out)
            EnableFocusEvents.writeAnsi(out)
            EnableSgrMouse.writeAnsi(out)
            if (mouseMotion) EnableMouseMotion.writeAnsi(out)
            if (kitty) EnableKittyKeyboard().writeAnsi(out)
            EnableInBandResize.writeAnsi(out)
        }
    }

    /**
     * Pairs with [EnableAll] — emit on shutdown to restore the terminal
     * cleanly. In addition to undoing every mode bit, this also resets a
     * handful of defaults so the shell prompt isn't left with the App's
     * cursor / SGR / scroll-region state: `CSI ?25h` (show cursor),
     * `CSI 0m` (SGR reset), `CSI r` (reset scroll region).
     */
    public data class DisableAll(
        val kitty: Boolean = true,
        val mouseMotion: Boolean = false,
    ) : Command {
        override fun writeAnsi(out: Appendable) {
            DisableInBandResize.writeAnsi(out)
            if (kitty) DisableKittyKeyboard.writeAnsi(out)
            if (mouseMotion) DisableMouseMotion.writeAnsi(out)
            DisableSgrMouse.writeAnsi(out)
            DisableFocusEvents.writeAnsi(out)
            DisableBracketedPaste.writeAnsi(out)
            // Safety resets for state the App may have changed but didn't track:
            out.append(CSI).append("?25h")   // show cursor
            out.append(CSI).append("0m")     // reset SGR (colors, bold, etc.)
            out.append(CSI).append("r")      // reset scroll region
        }
    }
}
