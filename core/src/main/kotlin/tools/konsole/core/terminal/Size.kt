package tools.konsole.core.terminal

/** Terminal dimensions in cells (columns, rows). */
public data class Size(val columns: Int, val rows: Int)

/**
 * Terminal dimensions including pixel size, where reported.
 * `widthPx`/`heightPx` are 0 if the terminal/host does not report pixel size.
 */
public data class WindowSize(
    val columns: Int,
    val rows: Int,
    val widthPx: Int,
    val heightPx: Int,
)
