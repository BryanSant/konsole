package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Console
import tools.konsole.rich.Renderable
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.table.Column as RichColumn
import tools.konsole.rich.table.Table as RichTable
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Scrollable
import tools.konsole.textual.widget.Widget

/**
 * What the cursor highlights in a [DataTable]. Mirrors textual's `CursorType`.
 */
public enum class CursorType { Cell, Row, Column, None }

/** A `(row, column)` index pair into a [DataTable]. */
public data class Coordinate(public val row: Int, public val column: Int) {
    public companion object { public val ZERO: Coordinate = Coordinate(0, 0) }
}

/**
 * Scrollable, navigable data grid. Mirrors Python textual's `DataTable`.
 *
 * Rendered via rich's [RichTable] for cell layout, with an overlay highlight
 * on the cursor cell/row/column (configurable via [cursorType]).
 *
 * Phase 9.6 features supported:
 *   - dynamic columns (id + label + width)
 *   - dynamic rows of arbitrary cell renderables
 *   - cursor navigation via arrow keys / page-up / page-down / home / end
 *   - addColumn / addRow / removeRow / clear
 *   - sort(columnId) — stable in-place sort
 *   - Highlighted / Selected / RowSelected messages
 *
 * Deferred to Phase 9.7+ (when the compositor wires real viewports):
 *   - column auto-resize
 *   - fixed header row pinning during scroll
 *   - row striping (left to CSS once the TCSS engine wires Styles → renderer)
 */
public open class DataTable(
    public val cursorType: CursorType = CursorType.Cell,
    public val showHeader: Boolean = true,
    public val zebraStripes: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Scrollable {

    public data class Column(public val id: String, public val label: Renderable, public val width: Int? = null)

    public data class Row(public val id: String, public val cells: List<Renderable>)

    private val _columns: MutableList<Column> = mutableListOf()
    private val _rows: MutableList<Row> = mutableListOf()

    public val columns: List<Column> get() = _columns.toList()
    public val rows: List<Row> get() = _rows.toList()

    public val rowCount: Int get() = _rows.size
    public val columnCount: Int get() = _columns.size

    public var cursorCoordinate: Coordinate = Coordinate.ZERO
        private set

    override val canFocus: Boolean get() = true

    override var scrollX: Int = 0
    override var scrollY: Int = 0
    override val contentWidth: Int get() = _columns.sumOf { it.width ?: 10 } + _columns.size + 1
    override val contentHeight: Int get() = _rows.size + (if (showHeader) 1 else 0)

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("up", "cursor_up", show = false),
            Binding("down", "cursor_down", show = false),
            Binding("left", "cursor_left", show = false),
            Binding("right", "cursor_right", show = false),
            Binding("home", "cursor_home", show = false),
            Binding("end", "cursor_end", show = false),
            Binding("pageup", "cursor_page_up", show = false),
            Binding("pagedown", "cursor_page_down", show = false),
            Binding("enter", "select", "Select"),
        )
    )

    /** Append a column. Returns the column id. */
    public open fun addColumn(label: String, id: String? = null, width: Int? = null): String {
        val cid = id ?: "col-${_columns.size}"
        _columns += Column(cid, Markup.parse(label), width)
        refresh()
        return cid
    }

    public open fun addColumn(column: Column): String {
        _columns += column
        refresh()
        return column.id
    }

    /** Append a row. Cells beyond [columnCount] are ignored; missing cells render empty. */
    public open fun addRow(vararg cells: Any?, id: String? = null): String {
        val rid = id ?: "row-${_rows.size}"
        val rendered = cells.map { c ->
            when (c) {
                is Renderable -> c
                is String -> Markup.parse(c)
                null -> Text("")
                else -> Text(c.toString())
            }
        }
        _rows += Row(rid, rendered)
        refresh()
        return rid
    }

    public open fun removeRow(rowId: String): Boolean {
        val removed = _rows.removeAll { it.id == rowId }
        if (removed) refresh()
        return removed
    }

    public open fun clear(includeColumns: Boolean = false) {
        _rows.clear()
        if (includeColumns) _columns.clear()
        cursorCoordinate = Coordinate.ZERO
        refresh()
    }

    /**
     * Stable in-place sort of rows by the value at [columnId].
     * Compares cells by their rendered plain text.
     */
    public open fun sort(columnId: String, descending: Boolean = false) {
        val colIdx = _columns.indexOfFirst { it.id == columnId }
        if (colIdx < 0) return
        val sorted = _rows.sortedBy { row ->
            val cell = row.cells.getOrNull(colIdx) ?: Text("")
            renderToString(cell)
        }
        _rows.clear()
        _rows.addAll(if (descending) sorted.reversed() else sorted)
        refresh()
    }

    public fun moveCursor(row: Int = cursorCoordinate.row, column: Int = cursorCoordinate.column) {
        val newRow = row.coerceIn(0, (_rows.size - 1).coerceAtLeast(0))
        val newCol = column.coerceIn(0, (_columns.size - 1).coerceAtLeast(0))
        val newCoord = Coordinate(newRow, newCol)
        if (newCoord == cursorCoordinate) return
        cursorCoordinate = newCoord
        post(Highlighted(this, newCoord))
        refresh()
    }

    public fun selectCurrent(): Boolean {
        val cell = currentCell() ?: return false
        post(Selected(this, cursorCoordinate, cell))
        if (cursorType == CursorType.Row) {
            val row = _rows.getOrNull(cursorCoordinate.row)
            if (row != null) post(RowSelected(this, cursorCoordinate.row, row.id))
        }
        return true
    }

    public fun currentCell(): Renderable? = _rows.getOrNull(cursorCoordinate.row)?.cells?.getOrNull(cursorCoordinate.column)

    override fun render(): Renderable {
        val table = RichTable(box = Box.SQUARE, showHeader = showHeader)
        for (col in _columns) table.addColumn(RichColumn(header = col.label))
        for ((rowIdx, row) in _rows.withIndex()) {
            val rendered: Array<Renderable> = _columns.mapIndexed { colIdx, _ ->
                val raw = row.cells.getOrNull(colIdx) ?: (Text("") as Renderable)
                applyCursorStyle(raw, rowIdx, colIdx)
            }.toTypedArray()
            table.addRow(*rendered)
        }
        return table
    }

    private fun applyCursorStyle(cell: Renderable, row: Int, column: Int): Renderable {
        if (cursorType == CursorType.None) return cell
        val highlight = when (cursorType) {
            CursorType.Cell -> row == cursorCoordinate.row && column == cursorCoordinate.column
            CursorType.Row -> row == cursorCoordinate.row
            CursorType.Column -> column == cursorCoordinate.column
            CursorType.None -> false
        }
        if (!highlight) return cell
        // Re-render the cell and overlay a highlight style.
        val plain = renderToString(cell)
        return Text(plain, style = Style(color = Color.Black, bgcolor = Color.Cyan, bold = true))
    }

    private fun renderToString(r: Renderable): String {
        val console = Console.string(width = 80)
        val opts = RenderOptions(maxWidth = 80)
        return r.render(console, opts).joinToString("") { it.text }
    }

    public data class Highlighted(val table: DataTable, val coordinate: Coordinate) : Message()
    public data class Selected(val table: DataTable, val coordinate: Coordinate, val cell: Renderable) : Message()
    public data class RowSelected(val table: DataTable, val rowIndex: Int, val rowId: String) : Message()
}
