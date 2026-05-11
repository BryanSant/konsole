package tools.konsole.examples

import tools.konsole.rich.geometry.Region
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.compositor.Compositor
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Digits
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header

/**
 * Four-function calculator. Buttons drive a state machine; the Digits
 * display reflects the current accumulator.
 *
 *   ./demo.sh Calculator
 *
 * Type digits 0–9 to enter numbers, `+ - * / =` for operators, AC (or "c") to
 * clear, and q to quit. Buttons are also clickable when running in a
 * terminal with mouse support.
 */
public class CalculatorApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val display = Digits("0", id = "display")

    // Calculator state
    @Volatile private var current: String = "0"
    @Volatile private var accumulator: Double = 0.0
    @Volatile private var pendingOp: Char? = null
    @Volatile private var freshEntry: Boolean = true

    private val digits = (0..9).map { d ->
        Button(d.toString(), id = "d$d")
    }
    private val opPlus = Button("+", variant = ButtonVariant.Warning, id = "op_plus")
    private val opMinus = Button("-", variant = ButtonVariant.Warning, id = "op_minus")
    private val opMul = Button("*", variant = ButtonVariant.Warning, id = "op_mul")
    private val opDiv = Button("/", variant = ButtonVariant.Warning, id = "op_div")
    private val opEquals = Button("=", variant = ButtonVariant.Primary, id = "op_eq")
    private val opClear = Button("AC", variant = ButtonVariant.Error, id = "op_clear")

    override val bindings = bindings(
        "q" to "quit",
        "ctrl+c" to "quit",
        "c" to "clear",
        "0" to "digit_0", "1" to "digit_1", "2" to "digit_2", "3" to "digit_3", "4" to "digit_4",
        "5" to "digit_5", "6" to "digit_6", "7" to "digit_7", "8" to "digit_8", "9" to "digit_9",
        "+" to "plus", "-" to "minus", "*" to "mul", "/" to "div",
        "=" to "equals", "enter" to "equals",
    )

    @Suppress("unused") public fun action_clear() { clearAll() }
    @Suppress("unused") public fun action_digit_0() = digit(0)
    @Suppress("unused") public fun action_digit_1() = digit(1)
    @Suppress("unused") public fun action_digit_2() = digit(2)
    @Suppress("unused") public fun action_digit_3() = digit(3)
    @Suppress("unused") public fun action_digit_4() = digit(4)
    @Suppress("unused") public fun action_digit_5() = digit(5)
    @Suppress("unused") public fun action_digit_6() = digit(6)
    @Suppress("unused") public fun action_digit_7() = digit(7)
    @Suppress("unused") public fun action_digit_8() = digit(8)
    @Suppress("unused") public fun action_digit_9() = digit(9)
    @Suppress("unused") public fun action_plus() = operator('+')
    @Suppress("unused") public fun action_minus() = operator('-')
    @Suppress("unused") public fun action_mul() = operator('*')
    @Suppress("unused") public fun action_div() = operator('/')
    @Suppress("unused") public fun action_equals() = equals()

    private val header = Header(title = "Calculator")
    private val footer = Footer(this.bindings)

    override fun compose(): Sequence<Widget> = sequenceOf(
        header,
        display,
    ) + digits.asSequence() + sequenceOf(
        opPlus, opMinus, opMul, opDiv, opEquals, opClear,
        footer,
    )

    override fun arrangeBaseLayer(widgets: List<Widget>) {
        val w = screenWidth
        val h = screenHeight
        // Header on top row, footer on bottom row.
        compositor.placeAt(header, Region(0, 0, w, 1), Compositor.BASE)
        compositor.placeAt(footer, Region(0, h - 1, w, 1), Compositor.BASE)

        // Big digit display under the header — 5 rows is the Digits font's natural height.
        val displayHeight = 5
        compositor.placeAt(display, Region(0, 1, w, displayHeight), Compositor.BASE)

        // 4×4 grid below the display. 16 buttons in the order a standard
        // calculator wants them; layout takes the remaining vertical space.
        val gridTopY = 1 + displayHeight + 1   // one-row gap below display
        val gridRows = 4
        val gridCols = 4
        val gridBottomY = h - 2                // one-row gap above footer
        val gridHeight = (gridBottomY - gridTopY).coerceAtLeast(gridRows)
        val cellH = (gridHeight / gridRows).coerceAtLeast(1)
        val cellW = (w / gridCols).coerceAtLeast(1)

        val layout: List<List<Button>> = listOf(
            listOf(digits[7], digits[8], digits[9], opDiv),
            listOf(digits[4], digits[5], digits[6], opMul),
            listOf(digits[1], digits[2], digits[3], opMinus),
            listOf(digits[0], opClear, opEquals, opPlus),
        )
        for ((r, row) in layout.withIndex()) {
            for ((c, btn) in row.withIndex()) {
                compositor.placeAt(
                    btn,
                    Region(c * cellW, gridTopY + r * cellH, cellW, cellH),
                    Compositor.BASE,
                )
            }
        }
    }

    init {
        for (b in digits) {
            b.start()
            val d = b.label.toInt()
            b.onMessage<Button.Pressed> { digit(d) }
        }
        opPlus.start();  opPlus.onMessage<Button.Pressed>  { operator('+') }
        opMinus.start(); opMinus.onMessage<Button.Pressed> { operator('-') }
        opMul.start();   opMul.onMessage<Button.Pressed>   { operator('*') }
        opDiv.start();   opDiv.onMessage<Button.Pressed>   { operator('/') }
        opEquals.start(); opEquals.onMessage<Button.Pressed> { equals() }
        opClear.start(); opClear.onMessage<Button.Pressed> { clearAll() }
    }

    private fun digit(d: Int) {
        current = if (freshEntry || current == "0") d.toString() else current + d.toString()
        freshEntry = false
        display.update(current)
        requestRefresh()
    }

    private fun operator(op: Char) {
        commitPending()
        pendingOp = op
        freshEntry = true
    }

    private fun equals() {
        commitPending()
        pendingOp = null
        freshEntry = true
        display.update(formatNumber(accumulator))
        current = display.value
        requestRefresh()
    }

    private fun commitPending() {
        val n = current.toDoubleOrNull() ?: 0.0
        accumulator = when (pendingOp) {
            null -> n
            '+' -> accumulator + n
            '-' -> accumulator - n
            '*' -> accumulator * n
            '/' -> if (n == 0.0) Double.NaN else accumulator / n
            else -> n
        }
    }

    private fun clearAll() {
        current = "0"
        accumulator = 0.0
        pendingOp = null
        freshEntry = true
        display.update("0")
        requestRefresh()
    }

    private fun formatNumber(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else "%g".format(v)

    /** Test-visible accessor for the current displayed value. */
    public val displayValue: String get() = display.value
}

public fun main() {
    CalculatorApp().run()
}
