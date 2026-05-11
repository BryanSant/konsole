package tools.konsole.core.terminal

/**
 * Specifies what region of the terminal should be cleared.
 * Mapped to ANSI `CSI N J` / `CSI N K` codes.
 */
public enum class ClearType {
    /** Clear the entire visible screen, leaving scrollback intact. */
    All,

    /** Clear screen and scrollback. */
    Purge,

    /** Clear from cursor down to end of screen. */
    FromCursorDown,

    /** Clear from start of screen up to cursor. */
    FromCursorUp,

    /** Clear the entire current line. */
    CurrentLine,

    /** Clear from cursor to end of current line. */
    UntilNewLine,
}
