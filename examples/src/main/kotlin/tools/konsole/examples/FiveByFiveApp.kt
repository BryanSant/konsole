package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Vertical

/**
 * 5x5 light-flip puzzle. Toggle a cell and its four orthogonal neighbours;
 * the goal is to fill every cell. Mirrors textual's `examples/five_by_five.py`.
 *
 *   ./gradlew :examples:runExample -Pexample=FiveByFiveApp
 *
 * Keys: h j k l / arrow keys to move, space to toggle, n for new game,
 * q to quit. Mouse click also works.
 */
public class FiveByFiveApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val grid = GameGrid()
    private val header = Header(title = "5x5 — A little annoying puzzle")
    private val footer = Footer(bindings = bindings(
        "n" to "new_game",
        "space" to "toggle",
        "arrows" to "navigate",
        "q" to "quit",
    ))

    override val bindings = bindings(
        "q" to "quit",
        "n" to "new_game",
        "space" to "toggle_cell",
        "up" to "move_up", "k" to "move_up", "w" to "move_up",
        "down" to "move_down", "j" to "move_down", "s" to "move_down",
        "left" to "move_left", "h" to "move_left", "a" to "move_left",
        "right" to "move_right", "l" to "move_right", "d" to "move_right",
    )

    init {
        grid.start()
        newGame()
    }

    private fun newGame() {
        grid.reset()
        // Start with a single toggle in the centre, matching the Python demo.
        grid.toggleAround(2, 2)
        grid.cursorRow = 2; grid.cursorCol = 2
        requestRefresh()
    }

    @Suppress("unused") public fun action_new_game() { newGame() }
    @Suppress("unused") public fun action_toggle_cell() {
        grid.toggleAround(grid.cursorRow, grid.cursorCol)
        requestRefresh()
    }
    @Suppress("unused") public fun action_move_up() { grid.move(-1, 0); requestRefresh() }
    @Suppress("unused") public fun action_move_down() { grid.move(1, 0); requestRefresh() }
    @Suppress("unused") public fun action_move_left() { grid.move(0, -1); requestRefresh() }
    @Suppress("unused") public fun action_move_right() { grid.move(0, 1); requestRefresh() }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(header, grid, footer),
            heights = listOf(
                Scalar(1.0, LengthUnit.Cells),
                Scalar(1.0, LengthUnit.Fraction),
                Scalar(1.0, LengthUnit.Cells),
            ),
        )
    )
}

private class GameGrid : Widget() {

    companion object { const val SIZE = 5 }

    /** Each cell is true (filled) or false (empty). */
    private val cells: Array<BooleanArray> = Array(SIZE) { BooleanArray(SIZE) }
    var cursorRow: Int = SIZE / 2
    var cursorCol: Int = SIZE / 2
    private var moves: Int = 0

    fun reset() {
        for (r in 0 until SIZE) cells[r].fill(false)
        moves = 0
    }

    val filledCount: Int get() = cells.sumOf { row -> row.count { it } }
    val won: Boolean get() = filledCount == SIZE * SIZE

    fun move(dr: Int, dc: Int) {
        cursorRow = ((cursorRow + dr) + SIZE) % SIZE
        cursorCol = ((cursorCol + dc) + SIZE) % SIZE
    }

    /** Toggle the cell at (r, c) plus its four orthogonal neighbours. */
    fun toggleAround(r: Int, c: Int) {
        if (won) return
        for ((dr, dc) in listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until SIZE && nc in 0 until SIZE) cells[nr][nc] = !cells[nr][nc]
        }
        moves += 1
    }

    override fun render(): Renderable {
        // Build a multi-line Text — one row per visual line of the grid.
        // Each cell is 5 columns wide and 3 rows tall (with borders).
        val cellWidth = 5
        val cellHeight = 3
        val text = Text()
        val filled = Style(bgcolor = Color.Rgb(64, 128, 200), color = Color.White)
        val empty = Style(bgcolor = Color.Rgb(48, 48, 48), color = Color.DarkGrey)
        val cursorFilled = Style(bgcolor = Color.Rgb(96, 192, 240), color = Color.White, bold = true)
        val cursorEmpty = Style(bgcolor = Color.Rgb(96, 96, 96), color = Color.Yellow, bold = true)
        val winFilled = Style(bgcolor = Color.Rgb(0, 180, 80), color = Color.White, bold = true)

        for (r in 0 until SIZE) {
            for (vy in 0 until cellHeight) {
                for (c in 0 until SIZE) {
                    val isCursor = (r == cursorRow && c == cursorCol)
                    val style = when {
                        won && cells[r][c] -> winFilled
                        cells[r][c] && isCursor -> cursorFilled
                        cells[r][c] -> filled
                        isCursor -> cursorEmpty
                        else -> empty
                    }
                    text.append(" ".repeat(cellWidth), style)
                }
                text.append("\n")
            }
        }
        val status = if (won)
            "  🎉 WINNER!  moves: $moves  (n: new game, q: quit)"
        else
            "  moves: $moves   filled: $filledCount / ${SIZE * SIZE}"
        text.append(status, Style(bold = true))
        return text
    }

    override fun renderLine(y: Int, width: Int): Strip {
        val flat = render().render(
            console = tools.konsole.rich.Console.string(width = width),
            options = tools.konsole.rich.RenderOptions(maxWidth = width),
        ).toList()
        // Split by newline segments.
        val lines = mutableListOf<MutableList<Segment>>(mutableListOf())
        for (seg in flat) {
            if (seg.text == "\n") lines.add(mutableListOf())
            else lines.last().add(seg)
        }
        val line = lines.getOrNull(y) ?: return Strip.EMPTY
        return Strip.of(line).adjustCellLength(width)
    }
}

public fun main() {
    FiveByFiveApp().run()
}
