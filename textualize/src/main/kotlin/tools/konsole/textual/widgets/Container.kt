package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Text
import tools.konsole.textual.css.LayoutKind
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.css.Styles
import tools.konsole.textual.widget.Widget

private val ONE_FR: Scalar = Scalar(1.0, LengthUnit.Fraction)

/**
 * A widget that arranges other widgets. Mirrors textual's `Container` mixin —
 * widgets that implement this surface their [containerStyles] (grid metadata,
 * etc.) and per-child sizing hints to the compositor, which dispatches to the
 * matching engine in [tools.konsole.textual.layouts.Layout].
 *
 * Concrete subclasses: [Vertical], [Horizontal], [Grid].
 */
public interface Container {
    /** Direction of child layout. */
    public val layout: LayoutKind

    /**
     * Container-level styles (grid-size, grid-rows, grid-columns, grid-gutter).
     * The non-grid layouts ignore these.
     */
    public val containerStyles: Styles

    /** The widgets this container arranges, in declaration order. */
    public val containerChildren: List<Widget>

    /**
     * Per-child sizing hints — width / height / column-span / row-span on a
     * child widget. Default: the empty [Styles] (engine picks `1fr` for
     * unspecified tracks). Override to declare a child's `1fr` height etc.
     */
    public fun childStyles(child: Widget): Styles = Styles.NULL
}

/**
 * Stack children vertically, top to bottom. Each child gets the full width
 * (minus margins) and the height declared in its [childStyles].
 *
 *   Vertical(header, body, footer)
 */
public open class Vertical(
    children: List<Widget> = emptyList(),
    /** Optional fixed heights / fractions, one per child. Defaults to all `1fr`. */
    private val heights: List<Scalar>? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Container {

    public constructor(vararg children: Widget, id: String? = null, classes: Set<String> = emptySet()) :
        this(children.toList(), null, id, classes)

    override val layout: LayoutKind get() = LayoutKind.Vertical
    override val containerStyles: Styles get() = Styles.NULL

    private val _children: MutableList<Widget> = children.toMutableList()
    override val containerChildren: List<Widget> get() = _children.toList()

    init { for (c in _children) attach(c) }

    /** Add a child at the end. */
    public fun add(widget: Widget) {
        _children += widget
        attach(widget)
        refresh()
    }

    override fun childStyles(child: Widget): Styles {
        val h = heights?.getOrNull(_children.indexOf(child)) ?: ONE_FR
        return Styles(height = h)
    }

    override fun render(): Renderable = Text("")  // children render themselves
}

/**
 * Stack children horizontally, left to right. Each child gets the full height
 * (minus margins) and the width declared in its [childStyles].
 */
public open class Horizontal(
    children: List<Widget> = emptyList(),
    /** Optional fixed widths / fractions, one per child. Defaults to all `1fr`. */
    private val widths: List<Scalar>? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Container {

    public constructor(vararg children: Widget, id: String? = null, classes: Set<String> = emptySet()) :
        this(children.toList(), null, id, classes)

    override val layout: LayoutKind get() = LayoutKind.Horizontal
    override val containerStyles: Styles get() = Styles.NULL

    private val _children: MutableList<Widget> = children.toMutableList()
    override val containerChildren: List<Widget> get() = _children.toList()

    init { for (c in _children) attach(c) }

    public fun add(widget: Widget) {
        _children += widget
        attach(widget)
        refresh()
    }

    override fun childStyles(child: Widget): Styles {
        val w = widths?.getOrNull(_children.indexOf(child)) ?: ONE_FR
        return Styles(width = w)
    }

    override fun render(): Renderable = Text("")
}

/**
 * Place children in a `cols × rows` grid. Mirrors textual's `Grid` and the
 * `layout: grid; grid-size: <cols> <rows>;` CSS pattern.
 *
 *   Grid(
 *       cols = 4, rows = 4,
 *       gutter = 1 to 1,
 *       children = listOf(btn1, btn2, …, btn16),
 *   )
 *
 * Track sizes (`gridColumns` / `gridRows`) default to all-`1fr`; override via
 * [columnSizes] and [rowSizes] to mix fixed cells, `%`, or `fr` units. Child
 * span comes from [Container.childStyles] — override the open
 * [Grid.cellSpan] method to map specific children to multi-cell positions.
 */
public open class Grid(
    public val cols: Int,
    public val rows: Int,
    /** Per-column sizing. Defaults to all `1fr`. */
    public val columnSizes: List<Scalar>? = null,
    /** Per-row sizing. Defaults to all `1fr`. */
    public val rowSizes: List<Scalar>? = null,
    /** Spacing between cells: `(horizontal, vertical)`. */
    public val gutter: Pair<Int, Int> = 0 to 0,
    children: List<Widget> = emptyList(),
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Container {

    override val layout: LayoutKind get() = LayoutKind.Grid

    override val containerStyles: Styles get() = Styles(
        gridSize = cols to rows,
        gridColumns = columnSizes,
        gridRows = rowSizes,
        gridGutter = gutter,
    )

    private val _children: MutableList<Widget> = children.toMutableList()
    override val containerChildren: List<Widget> get() = _children.toList()

    init { for (c in _children) attach(c) }

    public fun add(widget: Widget) {
        _children += widget
        attach(widget)
        refresh()
    }

    /**
     * Override to give specific children a non-default span. Receives the
     * child and returns `Pair<columnSpan, rowSpan>` (each ≥1). Default: `1, 1`.
     */
    public open fun cellSpan(child: Widget): Pair<Int, Int> = 1 to 1

    override fun childStyles(child: Widget): Styles {
        val (cs, rs) = cellSpan(child)
        return if (cs == 1 && rs == 1) Styles.NULL
        else Styles(columnSpan = cs, rowSpan = rs)
    }

    override fun render(): Renderable = Text("")
}
