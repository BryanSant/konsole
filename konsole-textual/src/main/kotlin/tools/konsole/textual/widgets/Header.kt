package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.layout.Align
import tools.konsole.rich.layout.Padded
import tools.konsole.rich.layout.Padding
import tools.konsole.rich.text.Justify
import tools.konsole.textual.widget.Widget

/**
 * Application chrome header bar. Mirrors Python textual's `Header`.
 *
 * Renders the app [title] (centered) and an optional [icon] on the left. By
 * default uses textual's `⭘` icon to match Python.
 *
 * @param title app title text. Defaults to "konsole" when blank.
 * @param icon glyph shown on the far left. Mirrors textual's `HeaderIcon.icon` default.
 * @param showIcon whether to render the icon.
 */
public open class Header(
    public val title: String = "konsole",
    public val icon: String = "⭘",
    public val showIcon: Boolean = true,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    private val style = Style(color = tools.konsole.core.style.Color.White, bgcolor = tools.konsole.core.style.Color.Blue, bold = true)

    override fun render(): Renderable {
        val text = Text()
        if (showIcon) text.append("$icon ", style)
        text.append(title, style)
        return Padded(Align(text, align = Justify.Center), Padding(top = 0, right = 1, bottom = 0, left = 1), style = style)
    }
}
