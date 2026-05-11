package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.textual.widget.Widget

/**
 * A wrapper that hosts a single child widget inside a [ListView] row.
 * Mirrors Python textual's `ListItem`.
 *
 * Lets `ListView` carry arbitrary widgets (Label, Button, custom cards) as
 * individually-selectable rows. The wrapper preserves the child's bindings
 * and event flow.
 *
 * @param child the widget rendered for this row.
 */
public open class ListItem(
    public val child: Widget,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    init { attach(child) }

    override fun render(): Renderable = child.render()
}
