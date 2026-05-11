package tools.konsole.core

/**
 * Terminal color capability. Detected from environment and TTY status; used by
 * higher-level renderers to downgrade RGB colors at emit time.
 *
 * Detection matrix (highest precedence first):
 * - `NO_COLOR` set (any non-empty value) → [None] (per https://no-color.org).
 * - `FORCE_COLOR` set → at least [Standard]; may be promoted by [COLORTERM] / [TERM].
 *   `FORCE_COLOR=0` is treated as [None]; `FORCE_COLOR=1` → [Standard];
 *   `FORCE_COLOR=2` → [EightBit]; `FORCE_COLOR=3` → [TrueColor].
 * - `COLORTERM=truecolor` or `24bit` → [TrueColor].
 * - `TERM` containing `truecolor`/`24bit` → [TrueColor].
 * - `TERM` containing `256color`/`256` → [EightBit].
 * - `TERM` starting with `xterm`/`screen`/`tmux`/`alacritty`/`kitty`/`ghostty`/`wezterm`/`vte`/`rxvt` → [EightBit].
 * - On Windows host with no other signal → [Windows].
 * - Otherwise on a TTY → [Standard]; off a TTY → [None].
 */
public enum class ColorSystem {
    /** No colors; renderers should emit plain text. */
    None,

    /** 16 named colors (8 base + 8 bright). */
    Standard,

    /** 256-color indexed palette (xterm-256). */
    EightBit,

    /** 24-bit truecolor (RGB). */
    TrueColor,

    /** Legacy Windows console (treated as [Standard] with safer ASCII boxes by renderers). */
    Windows,
    ;

    public companion object {
        /**
         * Detect the best [ColorSystem] for the running process.
         *
         * @param isTty whether stdout is connected to a TTY. Caller is responsible
         *   for determining this — typically `terminal.type != "dumb"` for a JLine
         *   terminal, or `System.console() != null` as a fallback.
         */
        public fun detect(isTty: Boolean): ColorSystem {
            val noColor = System.getenv("NO_COLOR")
            if (!noColor.isNullOrEmpty()) return None

            val forceColor = System.getenv("FORCE_COLOR")
            val forced: ColorSystem? = when (forceColor) {
                null, "" -> null
                "0", "false" -> None
                "1", "true" -> Standard
                "2" -> EightBit
                "3" -> TrueColor
                else -> Standard
            }

            val colorterm = System.getenv("COLORTERM")
            if (colorterm == "truecolor" || colorterm == "24bit") return TrueColor

            val term = System.getenv("TERM").orEmpty()

            if (forced != null) return forced

            if (term.isEmpty() || term == "dumb") return if (isTty) Standard else None

            val isWindowsHost = System.getProperty("os.name", "").startsWith("Windows", ignoreCase = true)

            return when {
                "truecolor" in term || "24bit" in term -> TrueColor
                "256color" in term || "256" in term -> EightBit
                term.startsWith("xterm") -> EightBit
                term.startsWith("screen") -> EightBit
                term.startsWith("tmux") -> EightBit
                term.startsWith("alacritty") -> TrueColor
                term.startsWith("kitty") -> TrueColor
                term.startsWith("ghostty") -> TrueColor
                term.startsWith("wezterm") -> TrueColor
                term.startsWith("vte") -> EightBit
                term.startsWith("rxvt") -> EightBit
                isWindowsHost -> Windows
                isTty -> Standard
                else -> None
            }
        }
    }
}
