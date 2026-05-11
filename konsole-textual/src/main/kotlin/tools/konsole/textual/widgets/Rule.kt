package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.layout.Rule as RichRule
import tools.konsole.textual.widget.Widget

/**
 * A horizontal divider. Mirrors Python textual's `Rule` — wraps rich's [RichRule].
 *
 * @param title optional centered title.
 * @param style style applied to the rule line.
 * @param char glyph used to draw the line (default `─`).
 */
public open class Rule(
    public val title: String? = null,
    public val ruleStyle: Style = Style.NULL,
    public val char: Char = '─',
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override fun render(): Renderable = RichRule(title = title, style = ruleStyle, char = char)
}
