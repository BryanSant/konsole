package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.pretty.Pretty as PrettyRenderable
import tools.konsole.textual.widget.Widget

/**
 * Widget wrapper around rich's [PrettyRenderable]. Mirrors Python textual's
 * `Pretty` widget — display arbitrary Kotlin objects with type-aware
 * coloring and live updates via [update].
 *
 * @param target initial object to display.
 * @param maxLength truncate containers wider than this. `null` = unbounded.
 * @param maxString truncate strings longer than this. `null` = unbounded.
 * @param maxDepth render at most this many levels. `null` = unbounded.
 * @param indentGuides draw vertical guides at each indent level.
 */
public open class PrettyWidget(
    target: Any? = null,
    public val maxLength: Int? = null,
    public val maxString: Int? = null,
    public val maxDepth: Int? = null,
    public val indentGuides: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var target: Any? = target
        private set

    public fun update(target: Any?) {
        this.target = target
        refresh()
    }

    override fun render(): Renderable = PrettyRenderable(
        target = target,
        maxLength = maxLength,
        maxString = maxString,
        maxDepth = maxDepth,
        indentGuides = indentGuides,
    )
}
