package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/**
 * A clickable hyperlink — renders as a styled text label that opens [url] when
 * the user clicks (or presses Enter while focused). Mirrors Python textual's
 * `Link` widget. Underneath uses OSC 8 so modern terminals render it as a
 * native clickable link; terminals without OSC 8 fall back to plain text.
 *
 * @param label visible link text. Defaults to [url] if blank.
 * @param url destination URL (https://, file://, mailto:, …).
 * @param style style for the link label (defaults to blue + underline).
 */
public open class Link(
    public val label: String = "",
    public val url: String,
    public val style: Style = Style(color = Color.Blue, underline = true),
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("enter", "open", "Open link"),
            Binding("space", "open", "Open link"),
        )
    )

    /** Emit [Activated] — UIs that hook this can shell out to a browser. */
    public fun activate(): Boolean = post(Activated(this, url))

    override fun render(): Renderable {
        val visible = label.ifEmpty { url }
        val t = Text()
        t.append(visible, style.copy(link = url))
        return t
    }

    /**
     * Posted when the link is activated (click / Enter / Space / programmatic [activate]).
     * Apps that want to open the URL via `xdg-open` / `open` / `start` should subscribe.
     */
    public data class Activated(val link: Link, val url: String) : Message()
}
