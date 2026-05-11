package tools.konsole.examples

import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.pilot.Pilot
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
 *   ./gradlew :examples:runExample -Pexample=CalculatorApp
 *
 * The bundled Pilot script computes `12 + 7 = 19` by clicking buttons.
 */
public class CalculatorApp : App(HeadlessDriver()) {

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
        "c" to "clear",
        "0" to "digit_0",
        "+" to "plus",
        "=" to "equals",
    )

    override fun compose(): Sequence<Widget> = sequenceOf(
        Header(title = "Calculator"),
        display,
    ) + digits.asSequence() + sequenceOf(
        opPlus, opMinus, opMul, opDiv, opEquals, opClear,
        Footer(this.bindings),
    )

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

public fun main(): Unit = runBlocking {
    val app = CalculatorApp()
    val pilot = Pilot(app)
    pilot.use { p ->
        p.pause(100)
        app.renderFrame()

        // Compute 12 + 7 = 19
        @Suppress("UNCHECKED_CAST")
        fun btn(id: String): Button = pilot.findOne("#$id") as? Button ?: error("missing #$id")
        btn("d1").press(); p.pause(20)
        btn("d2").press(); p.pause(20)
        btn("op_plus").press(); p.pause(20)
        btn("d7").press(); p.pause(20)
        btn("op_eq").press(); p.pause(50)
        app.renderFrame()
    }

    val console = Console.system()
    console.print("[bold]Calculator demo finished.[/]")
    console.print("Final display: [bold cyan]${app.displayValue}[/]")
    console.print("[dim](expected: 19)[/]")
}
