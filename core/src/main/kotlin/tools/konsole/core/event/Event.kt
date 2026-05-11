package tools.konsole.core.event

/** A terminal event delivered by [tools.konsole.core.Terminal.events]. */
public sealed interface Event {

    /** Terminal window gained focus (CSI I). */
    public data object FocusGained : Event

    /** Terminal window lost focus (CSI O). */
    public data object FocusLost : Event

    /** A keyboard event. */
    public data class Key(val event: KeyEvent) : Event {
        public constructor(code: KeyCode, modifiers: KeyModifiers = KeyModifiers.NONE) :
            this(KeyEvent(code, modifiers))
    }

    /** A mouse event. */
    public data class Mouse(val event: MouseEvent) : Event

    /** A bracketed-paste payload (between CSI 200~ and CSI 201~). */
    public data class Paste(val text: String) : Event

    /** Terminal resized; new dimensions in cells (columns, rows). */
    public data class Resize(val columns: Int, val rows: Int) : Event

    /** Terminal-reported foreground color from `OSC 10 ; ?` query. RGB components are 0-255. */
    public data class ForegroundColor(val r: Int, val g: Int, val b: Int) : Event

    /** Terminal-reported background color from `OSC 11 ; ?` query. RGB components are 0-255. */
    public data class BackgroundColor(val r: Int, val g: Int, val b: Int) : Event

    /** Terminal-reported palette color from `OSC 4 ; N ; ?` query. RGB components are 0-255. */
    public data class PaletteColor(val index: Int, val r: Int, val g: Int, val b: Int) : Event

    /**
     * Reply to a `CSI ? <mode> $ p` query, also known as DECRPM. Indicates whether
     * the queried mode is supported and currently set.
     */
    public data class ModeReport(val mode: Int, val state: ModeState) : Event

    /** Reply to a `CSI 6 n` cursor-position query. Zero-indexed. */
    public data class CursorPosition(val column: Int, val row: Int) : Event

    public val isKeyPress: Boolean
        get() = this is Key && event.kind == KeyEventKind.Press

    public val isKeyRelease: Boolean
        get() = this is Key && event.kind == KeyEventKind.Release

    public val isKeyRepeat: Boolean
        get() = this is Key && event.kind == KeyEventKind.Repeat
}
