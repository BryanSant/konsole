package tools.konsole.core.event

/**
 * Whether a key was pressed, repeated, or released.
 *
 * Only `Press` is delivered unless Kitty `REPORT_EVENT_TYPES` is enabled.
 */
public enum class KeyEventKind {
    Press,
    Repeat,
    Release,
}
