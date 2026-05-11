package tools.konsole.rich.table

import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.text.Justify
import tools.konsole.rich.text.Overflow

/**
 * A single column in a [Table]. Mirrors `rich.table.Column`.
 *
 * `width` (fixed) wins over `ratio` (proportional share of leftover width).
 * If both are null, the column auto-sizes to fit its widest cell.
 */
public class Column(
    public val header: Renderable? = null,
    public val footer: Renderable? = null,
    public val headerStyle: Style? = null,
    public val footerStyle: Style? = null,
    public val style: Style? = null,
    public val justify: Justify = Justify.Left,
    public val verticalAlign: tools.konsole.rich.layout.VerticalAlign = tools.konsole.rich.layout.VerticalAlign.Top,
    public val overflow: Overflow = Overflow.Ellipsis,
    public val width: Int? = null,
    public val minWidth: Int? = null,
    public val maxWidth: Int? = null,
    public val ratio: Int? = null,
    public val noWrap: Boolean = false,
) {
    internal val cells: MutableList<Renderable> = mutableListOf()
}
