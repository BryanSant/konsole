package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Align
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.text.Justify
import tools.konsole.textual.widget.Widget

/**
 * Decorative intro panel — the konsole equivalent of textual's `Welcome`
 * widget. Renders a centered title + body inside a rounded panel.
 *
 * Convenient when an app wants a friendly empty-state.
 *
 * @param title large heading (markup-parsed).
 * @param body subtitle/body paragraph (markup-parsed).
 */
public open class Welcome(
    public val title: String = "Welcome to konsole",
    public val body: String = "[dim]Press [bold cyan]?[/] for help.[/]",
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override fun render(): Renderable {
        val inner = Text()
        val titleStyle = Style(color = Color.Cyan, bold = true)
        // Big-but-not-Digits title (single-row, large emphasis).
        for (seg in Markup.parse(title).render(
            tools.konsole.rich.Console.string(width = 80),
            tools.konsole.rich.RenderOptions(maxWidth = 80),
        )) {
            inner.append(seg.text, (seg.style ?: Style.NULL) + titleStyle)
        }
        inner.append("\n\n")
        for (seg in Markup.parse(body).render(
            tools.konsole.rich.Console.string(width = 80),
            tools.konsole.rich.RenderOptions(maxWidth = 80),
        )) {
            inner.append(seg.text, seg.style)
        }
        return Panel(
            renderable = Align(inner, align = Justify.Center),
            box = Box.ROUNDED,
            borderStyle = Style(color = Color.Cyan),
        )
    }
}
