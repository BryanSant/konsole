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
import tools.konsole.textual.widgets.Vertical

/**
 * Merlin hand-held puzzle — toggle one switch and a cluster of neighbours
 * flip with it. Win when all switches except the centre are on.
 * Mirrors textual's `examples/merlin.py`.
 *
 *   ./gradlew :examples:runExample -Pexample=MerlinApp
 *
 * Press digit keys 1–9 to toggle a switch, n for a new game, q to quit.
 * (LinearGradient background from the Python version is omitted; konsole's
 * Static-color backgrounds stand in.)
 */
public class MerlinApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val board = MerlinBoard()
    private val footer = Footer(bindings = bindings(
        "1–9" to "toggle",
        "n" to "new_game",
        "q" to "quit",
    ))

    override val bindings = bindings(
        "q" to "quit",
        "n" to "new_game",
        "1" to "toggle_1", "2" to "toggle_2", "3" to "toggle_3",
        "4" to "toggle_4", "5" to "toggle_5", "6" to "toggle_6",
        "7" to "toggle_7", "8" to "toggle_8", "9" to "toggle_9",
    )

    init {
        board.start()
        board.randomize()
    }

    @Suppress("unused") public fun action_new_game() { board.randomize(); requestRefresh() }
    @Suppress("unused") public fun action_toggle_1() { board.press(1); requestRefresh() }
    @Suppress("unused") public fun action_toggle_2() { board.press(2); requestRefresh() }
    @Suppress("unused") public fun action_toggle_3() { board.press(3); requestRefresh() }
    @Suppress("unused") public fun action_toggle_4() { board.press(4); requestRefresh() }
    @Suppress("unused") public fun action_toggle_5() { board.press(5); requestRefresh() }
    @Suppress("unused") public fun action_toggle_6() { board.press(6); requestRefresh() }
    @Suppress("unused") public fun action_toggle_7() { board.press(7); requestRefresh() }
    @Suppress("unused") public fun action_toggle_8() { board.press(8); requestRefresh() }
    @Suppress("unused") public fun action_toggle_9() { board.press(9); requestRefresh() }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = listOf(board, footer),
            heights = listOf(
                Scalar(1.0, LengthUnit.Fraction),    // board takes all remaining space
                Scalar(1.0, LengthUnit.Cells),       // footer pinned to 1 row
            ),
        )
    )
}

private class MerlinBoard : Widget() {

    /** Same toggle map as textual/examples/merlin.py — pressing N flips these too. */
    private val toggles: Map<Int, List<Int>> = mapOf(
        1 to listOf(2, 4, 5),
        2 to listOf(1, 3),
        3 to listOf(2, 5, 6),
        4 to listOf(1, 7),
        5 to listOf(2, 4, 6, 8),
        6 to listOf(3, 9),
        7 to listOf(4, 5, 8),
        8 to listOf(7, 9),
        9 to listOf(5, 6, 8),
    )

    private val switches = BooleanArray(9)
    private var startMs = System.currentTimeMillis()
    private var elapsedAtWinMs: Long? = null

    val won: Boolean get() {
        // Goal: every switch except #5 is on (5 is the always-disabled centre in the Python demo).
        for (i in switches.indices) {
            val isOn = switches[i]
            val targetOn = (i + 1) != 5
            if (isOn != targetOn) return false
        }
        return true
    }

    fun randomize() {
        val rng = java.util.Random()
        for (i in switches.indices) switches[i] = rng.nextBoolean()
        startMs = System.currentTimeMillis()
        elapsedAtWinMs = null
    }

    fun press(n: Int) {
        if (won) return
        switches[n - 1] = !switches[n - 1]
        for (other in toggles[n].orEmpty()) {
            switches[other - 1] = !switches[other - 1]
        }
        if (won) elapsedAtWinMs = System.currentTimeMillis() - startMs
    }

    override fun render(): Renderable {
        val text = Text()
        val title = Style(color = Color.Cyan, bold = true)
        val timer = Style(color = if (won) Color.Green else Color.Yellow, bold = true)
        text.append("\n  M E R L I N\n", title)
        val seconds = ((elapsedAtWinMs ?: (System.currentTimeMillis() - startMs)) / 1000).toInt()
        text.append("  %02d:%02d\n\n".format(seconds / 60, seconds % 60), timer)

        // 3x3 grid laid out as switches 7-8-9 / 4-5-6 / 1-2-3 (number-pad order, matching Python).
        val rows = listOf(listOf(7, 8, 9), listOf(4, 5, 6), listOf(1, 2, 3))
        val onStyle = Style(color = Color.Green, bold = true)
        val offStyle = Style(color = Color.DarkGrey)
        val labelStyle = Style(color = Color.White)
        for (row in rows) {
            text.append("  ")
            for (n in row) {
                val on = switches[n - 1]
                text.append("[$n] ", labelStyle)
                if (on) text.append("[●]  ", onStyle) else text.append("[ ]  ", offStyle)
            }
            text.append("\n")
        }
        text.append("\n")
        if (won) {
            text.append("  You win! (n for a new game)\n", Style(color = Color.Green, bold = true))
        }
        return text
    }

    override fun renderLine(y: Int, width: Int): Strip {
        val flat = render().render(
            console = tools.konsole.rich.Console.string(width = width),
            options = tools.konsole.rich.RenderOptions(maxWidth = width),
        ).toList()
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
    MerlinApp().run()
}
