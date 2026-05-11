package tools.konsole.textual.theme

import tools.konsole.core.style.Color

/**
 * Application-level color palette. Mirrors Python textual's `Theme` (`textual/theme.py`)
 * — distinct from [tools.konsole.rich.Theme] which is rich's style-name lookup.
 *
 * A theme provides semantic color slots that widgets reference instead of
 * hard-coding colors:
 *
 * - [primary], [secondary], [accent] — brand colors
 * - [foreground], [background] — body text and surface
 * - [surface], [panel] — container backgrounds
 * - [success], [warning], [error] — status colors
 *
 * Themes are switched via `App.theme = …`; widgets re-render on the change.
 */
public data class Theme(
    public val name: String,
    public val dark: Boolean,
    public val primary: Color,
    public val secondary: Color,
    public val accent: Color,
    public val foreground: Color,
    public val background: Color,
    public val surface: Color,
    public val panel: Color,
    public val success: Color,
    public val warning: Color,
    public val error: Color,
    public val boost: Color = Color.Reset,
) {
    public companion object {
        public val DARK: Theme = Theme(
            name = "textual-dark",
            dark = true,
            primary = Color.Rgb(0x00, 0x4f, 0x9f),
            secondary = Color.Rgb(0xff, 0x55, 0x00),
            accent = Color.Rgb(0xff, 0xa6, 0x2b),
            foreground = Color.Rgb(0xe0, 0xe0, 0xe0),
            background = Color.Rgb(0x1e, 0x1e, 0x1e),
            surface = Color.Rgb(0x24, 0x27, 0x33),
            panel = Color.Rgb(0x2f, 0x33, 0x3d),
            success = Color.Rgb(0x4e, 0xc9, 0x4f),
            warning = Color.Rgb(0xe9, 0x9d, 0x42),
            error = Color.Rgb(0xe5, 0x5c, 0x5c),
        )

        public val LIGHT: Theme = Theme(
            name = "textual-light",
            dark = false,
            primary = Color.Rgb(0x00, 0x4f, 0x9f),
            secondary = Color.Rgb(0xff, 0x55, 0x00),
            accent = Color.Rgb(0xff, 0xa6, 0x2b),
            foreground = Color.Rgb(0x1f, 0x1f, 0x1f),
            background = Color.Rgb(0xff, 0xff, 0xff),
            surface = Color.Rgb(0xee, 0xee, 0xee),
            panel = Color.Rgb(0xdd, 0xdd, 0xdd),
            success = Color.Rgb(0x4e, 0xc9, 0x4f),
            warning = Color.Rgb(0xe9, 0x9d, 0x42),
            error = Color.Rgb(0xe5, 0x5c, 0x5c),
        )
    }
}
