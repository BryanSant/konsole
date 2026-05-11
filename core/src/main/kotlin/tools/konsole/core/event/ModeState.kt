package tools.konsole.core.event

/**
 * State value reported by DECRPM (`CSI ? <mode> ; <state> $ y`) in response to a
 * DECRQM query (`CSI ? <mode> $ p`). Mirrors the DEC private-mode reply table.
 */
public enum class ModeState(public val code: Int) {
    /** The mode is not recognized by the terminal. */
    NotRecognized(0),

    /** The mode is set (enabled). */
    Set(1),

    /** The mode is reset (disabled). */
    Reset(2),

    /** The mode is permanently set; SM/RM commands have no effect. */
    PermanentlySet(3),

    /** The mode is permanently reset; SM/RM commands have no effect. */
    PermanentlyReset(4),
    ;

    public companion object {
        public fun fromCode(code: Int): ModeState = entries.firstOrNull { it.code == code } ?: NotRecognized
    }
}
