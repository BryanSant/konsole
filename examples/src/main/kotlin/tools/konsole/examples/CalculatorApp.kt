package tools.konsole.examples

import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.ButtonVariant
import tools.konsole.textual.widgets.Digits
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Grid
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Vertical

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

    private val buttonGrid = Grid(
        cols = 4, rows = 4,
        gutter = 1 to 0,
        children = listOf(
            digits[7], digits[8], digits[9], opDiv,
            digits[4], digits[5], digits[6], opMul,
            digits[1], digits[2], digits[3], opMinus,
            digits[0], opClear, opEquals, opPlus,
        ),
        id = "buttons",
    )

    // Top-level vertical layout: header (1 row), display (5 rows), button grid
    // (remaining space), footer (1 row).
    private val root = Vertical(
        children = listOf(header, display, buttonGrid, footer),
        heights = listOf(
            Scalar(1.0, LengthUnit.Cells),
            Scalar(5.0, LengthUnit.Cells),
            Scalar(1.0, LengthUnit.Fraction),
            Scalar(1.0, LengthUnit.Cells),
        ),
        id = "root",
    )

    override fun compose(): Sequence<Widget> = sequenceOf(root)

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
