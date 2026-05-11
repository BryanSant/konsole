package tools.konsole.core.event

/** A mouse button. */
public enum class MouseButton {
    Left, Right, Middle,
}

/** What kind of mouse action occurred. */
public sealed interface MouseEventKind {
    public data class Down(val button: MouseButton) : MouseEventKind
    public data class Up(val button: MouseButton) : MouseEventKind
    public data class Drag(val button: MouseButton) : MouseEventKind
    public data object Moved : MouseEventKind
    public data object ScrollUp : MouseEventKind
    public data object ScrollDown : MouseEventKind
    public data object ScrollLeft : MouseEventKind
    public data object ScrollRight : MouseEventKind
}

/**
 * A mouse event with cell coordinates and active modifiers.
 *
 * Coordinates are 0-indexed.
 */
public data class MouseEvent(
    val kind: MouseEventKind,
    val column: Int,
    val row: Int,
    val modifiers: KeyModifiers = KeyModifiers.NONE,
)
