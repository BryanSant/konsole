package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Vertical

/**
 * Pride flag — six horizontal stripes filling the viewport. Mirrors textual's
 * `examples/pride.py`.
 *
 *   ./demo.sh Pride
 *
 * Press `q` to quit.
 */
public class PrideApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    override val bindings = bindings("q" to "quit")

    // Six stripes stacked vertically, each 1fr — the Vertical container splits
    // the viewport evenly. No imperative arrangeBaseLayer needed.
    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            Stripe(Color.Rgb(228, 3, 3)),     // red
            Stripe(Color.Rgb(255, 140, 0)),   // orange
            Stripe(Color.Rgb(255, 237, 0)),   // yellow
            Stripe(Color.Rgb(0, 128, 38)),    // green
            Stripe(Color.Rgb(0, 77, 255)),    // blue
            Stripe(Color.Rgb(117, 7, 135)),   // purple
        )
    )
}

private class Stripe(private val color: Color) : Widget() {
    override fun render(): Renderable = Text("")
    override fun renderLine(y: Int, width: Int): Strip =
        Strip.of(Segment(" ".repeat(width), Style(bgcolor = color)))
}

public fun main() {
    PrideApp().run()
}
