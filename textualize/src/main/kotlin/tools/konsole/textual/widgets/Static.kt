package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup
import tools.konsole.textual.widget.Widget

/**
 * Base widget for displaying static content — a [Renderable] (Panel/Table/Text/etc.)
 * or markup [String]. Mirrors Python textual's `Static`.
 *
 * Call [update] to replace the content. The widget renders whatever [content]
 * holds; subclasses (Label, ProgressBar, etc.) extend this and supply their own
 * default content.
 */
public open class Static(
    initialContent: Any = "",
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    /** Current content — a [Renderable] or markup [String]. */
    public var content: Any = initialContent
        protected set

    /** Replace the content and trigger a refresh. */
    public open fun update(content: Any) {
        this.content = content
        refresh()
    }

    override fun render(): Renderable = when (val c = content) {
        is Renderable -> c
        is String -> Markup.parse(c)
        else -> Text(c.toString())
    }
}
