package tools.konsole.core.event

/**
 * The actual modifier key that triggered an event (used as the *key* in a
 * [KeyCode.Modifier] event, distinct from the modifier *bitset* held in [KeyModifiers]).
 */
public enum class ModifierKeyCode {
    LeftShift, LeftControl, LeftAlt, LeftSuper, LeftHyper, LeftMeta,
    RightShift, RightControl, RightAlt, RightSuper, RightHyper, RightMeta,
    IsoLevel3Shift, IsoLevel5Shift,
}
