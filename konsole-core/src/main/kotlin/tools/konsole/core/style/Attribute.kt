package tools.konsole.core.style

/**
 * SGR (Select Graphic Rendition) text attributes.
 *
 * Each variant carries its `set` SGR code; the [reset] property gives the
 * matching turn-off code where one exists (else `null`, in which case the
 * `Reset` attribute (SGR 0) clears all attributes at once).
 *
 * Mirrors crossterm's `Attribute` enum (src/style/types/attribute.rs).
 */
public enum class Attribute(public val sgr: Int, public val reset: Int?) {
    Reset(0, null),

    Bold(1, 22),
    Dim(2, 22),
    Italic(3, 23),
    Underlined(4, 24),
    DoubleUnderlined(21, 24),
    Undercurled(4, 24),                  // SGR 4:3 in extended form; we emit the common 4
    Underdotted(4, 24),
    Underdashed(4, 24),
    SlowBlink(5, 25),
    RapidBlink(6, 25),
    Reverse(7, 27),
    Hidden(8, 28),
    CrossedOut(9, 29),
    Fraktur(20, 23),

    NoBold(22, null),
    NormalIntensity(22, null),
    NoItalic(23, null),
    NoUnderline(24, null),
    NoBlink(25, null),
    NoReverse(27, null),
    NoHidden(28, null),
    NotCrossedOut(29, null),

    Framed(51, 54),
    Encircled(52, 54),
    OverLined(53, 55),
    NotFramedOrEncircled(54, null),
    NotOverLined(55, null),
}
