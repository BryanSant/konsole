package tools.konsole.core.event

/**
 * A keyboard key. Most variants are singletons; [Char], [F], [Media], and
 * [Modifier] carry data so are data classes.
 *
 * Keys above [Insert] (`CapsLock`, `Pause`, etc.) are only delivered when the
 * Kitty `DISAMBIGUATE_ESCAPE_CODES` flag is active.
 */
public sealed interface KeyCode {

    public data object Backspace : KeyCode
    public data object Enter : KeyCode
    public data object Left : KeyCode
    public data object Right : KeyCode
    public data object Up : KeyCode
    public data object Down : KeyCode
    public data object Home : KeyCode
    public data object End : KeyCode
    public data object PageUp : KeyCode
    public data object PageDown : KeyCode
    public data object Tab : KeyCode
    public data object BackTab : KeyCode
    public data object Delete : KeyCode
    public data object Insert : KeyCode

    /** Function key. `F(1)` = F1, etc. */
    public data class F(val n: Int) : KeyCode

    /** Printable character key. */
    public data class Char(val c: kotlin.Char) : KeyCode

    public data object Null : KeyCode
    public data object Esc : KeyCode

    public data object CapsLock : KeyCode
    public data object ScrollLock : KeyCode
    public data object NumLock : KeyCode
    public data object PrintScreen : KeyCode
    public data object Pause : KeyCode
    public data object Menu : KeyCode
    public data object KeypadBegin : KeyCode

    public data class Media(val code: MediaKeyCode) : KeyCode
    public data class Modifier(val code: ModifierKeyCode) : KeyCode
}
