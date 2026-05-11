package tools.konsole.textual.css

import tools.konsole.core.style.Color
import tools.konsole.rich.Style as RichStyle
import tools.konsole.rich.geometry.Spacing

/** Top-level display mode for a widget. Mirrors textual's `Display`. */
public enum class Display { Block, None }

/** Visibility — visible vs hidden but still allocating space. */
public enum class Visibility { Visible, Hidden }

/** Layout direction. Mirrors textual's `Layout` enum (vertical/horizontal/grid). */
public enum class LayoutKind { Vertical, Horizontal, Grid }

/** Horizontal alignment within a region. */
public enum class AlignHorizontal { Left, Center, Right }

/** Vertical alignment within a region. */
public enum class AlignVertical { Top, Middle, Bottom }

/** Box-drawing border style. Subset of textual's `border.py`. */
public enum class BorderStyle {
    None, Solid, Round, Heavy, Double, Ascii, Dashed, Hidden
}

/**
 * The applied style rules on a widget — the merged output of all matching CSS [RuleSet]s.
 *
 * Mirrors a *useful subset* of Python textual's `Styles` (which has 100+ properties).
 * Phase 8 ships the properties most apps need; the rest land in subsequent passes.
 *
 * All fields default to `null` meaning "not set / inherit". The active values are
 * resolved by layout/compositor via [resolve] which fills in inherited or default values.
 */
public data class Styles(
    public val display: Display? = null,
    public val visibility: Visibility? = null,
    public val layout: LayoutKind? = null,

    public val color: Color? = null,
    public val background: Color? = null,
    public val textStyle: RichStyle? = null,

    public val width: Scalar? = null,
    public val height: Scalar? = null,
    public val minWidth: Scalar? = null,
    public val minHeight: Scalar? = null,
    public val maxWidth: Scalar? = null,
    public val maxHeight: Scalar? = null,

    public val padding: Spacing? = null,
    public val margin: Spacing? = null,

    public val border: Pair<BorderStyle, Color>? = null,
    public val borderTitle: String? = null,

    public val align: Pair<AlignHorizontal, AlignVertical>? = null,
    public val alignHorizontal: AlignHorizontal? = null,
    public val alignVertical: AlignVertical? = null,

    public val dock: Dock? = null,
    public val layer: String? = null,
    public val layers: List<String>? = null,

    public val opacity: Double? = null,

    // ---- Grid layout (only meaningful when [layout] is [LayoutKind.Grid]) ----
    /** `grid-size: <cols> <rows>` — `<rows>` defaults to 1 when omitted. */
    public val gridSize: Pair<Int, Int>? = null,
    /** `grid-rows: <track-list>` — height of each row track. */
    public val gridRows: List<Scalar>? = null,
    /** `grid-columns: <track-list>` — width of each column track. */
    public val gridColumns: List<Scalar>? = null,
    /** `grid-gutter: <h> <v>` — spacing between cells (horizontal, vertical). */
    public val gridGutter: Pair<Int, Int>? = null,
    /** `column-span: <int>` — number of columns this child occupies. Default 1. */
    public val columnSpan: Int? = null,
    /** `row-span: <int>` — number of rows this child occupies. Default 1. */
    public val rowSpan: Int? = null,
) {
    /** Right-side fields override left-side; nulls fall through. Mirrors CSS rule overlay. */
    public operator fun plus(other: Styles): Styles = Styles(
        display = other.display ?: display,
        visibility = other.visibility ?: visibility,
        layout = other.layout ?: layout,
        color = other.color ?: color,
        background = other.background ?: background,
        textStyle = other.textStyle ?: textStyle,
        width = other.width ?: width,
        height = other.height ?: height,
        minWidth = other.minWidth ?: minWidth,
        minHeight = other.minHeight ?: minHeight,
        maxWidth = other.maxWidth ?: maxWidth,
        maxHeight = other.maxHeight ?: maxHeight,
        padding = other.padding ?: padding,
        margin = other.margin ?: margin,
        border = other.border ?: border,
        borderTitle = other.borderTitle ?: borderTitle,
        align = other.align ?: align,
        alignHorizontal = other.alignHorizontal ?: alignHorizontal,
        alignVertical = other.alignVertical ?: alignVertical,
        dock = other.dock ?: dock,
        layer = other.layer ?: layer,
        layers = other.layers ?: layers,
        opacity = other.opacity ?: opacity,
        gridSize = other.gridSize ?: gridSize,
        gridRows = other.gridRows ?: gridRows,
        gridColumns = other.gridColumns ?: gridColumns,
        gridGutter = other.gridGutter ?: gridGutter,
        columnSpan = other.columnSpan ?: columnSpan,
        rowSpan = other.rowSpan ?: rowSpan,
    )

    public companion object {
        public val NULL: Styles = Styles()
    }
}

/** Dock edges. */
public enum class Dock { Top, Right, Bottom, Left }

/**
 * One `property: value;` line inside a TCSS rule.
 * Mirrors textual's `model.Declaration`.
 */
public data class Declaration(
    public val name: String,
    public val value: String,
    public val important: Boolean = false,
    public val line: Int = 0,
)

/**
 * A complete `selector { declaration; declaration; … }` block.
 * Mirrors textual's `model.RuleSet`.
 */
public data class RuleSet(
    public val selectors: SelectorSet,
    public val declarations: List<Declaration>,
    public val source: String = "",
) {
    public val specificity: Specificity get() = selectors.specificity
}
