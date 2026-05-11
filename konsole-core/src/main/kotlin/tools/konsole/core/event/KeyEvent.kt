package tools.konsole.core.event

/**
 * A keyboard event.
 *
 * @property code the key
 * @property modifiers held modifiers
 * @property kind press / repeat / release (always `Press` unless Kitty `REPORT_EVENT_TYPES`)
 * @property state extra state (keypad / caps / num) when reported by the terminal
 */
public data class KeyEvent(
    val code: KeyCode,
    val modifiers: KeyModifiers = KeyModifiers.NONE,
    val kind: KeyEventKind = KeyEventKind.Press,
    val state: KeyEventState = KeyEventState.NONE,
) {
    public companion object {
        /** Convenience: a press event with no modifiers and no state. */
        public fun press(code: KeyCode, modifiers: KeyModifiers = KeyModifiers.NONE): KeyEvent =
            KeyEvent(code, modifiers, KeyEventKind.Press, KeyEventState.NONE)
    }
}
