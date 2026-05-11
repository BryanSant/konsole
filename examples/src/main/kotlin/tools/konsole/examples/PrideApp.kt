package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.Style
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.compositor.Compositor
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget

/**
 * Pride flag — six horizontal stripes filling the viewport. Mirrors textual's
 * `examples/pride.py`.
 *
 *   ./gradlew :examples:runExample -Pexample=PrideApp
 *
 * Press `q` to quit.
 */
public class PrideApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val flag = PrideFlag()

    override val bindings = bindings("q" to "quit")

    override fun compose(): Sequence<Widget> = sequenceOf(flag)

    override fun arrangeBaseLayer(widgets: List<Widget>) {
        // Single full-screen widget; default vertical layout would give it 1 row.
        compositor.placeAt(flag, Region(0, 0, screenWidth, screenHeight), Compositor.BASE)
    }
}

/** Single full-screen widget that paints six equal-height pride stripes. */
private class PrideFlag : Widget() {

    private val stripes = listOf(
        Color.Rgb(228, 3, 3),     // red
        Color.Rgb(255, 140, 0),   // orange
        Color.Rgb(255, 237, 0),   // yellow
        Color.Rgb(0, 128, 38),    // green
        Color.Rgb(0, 77, 255),    // blue
        Color.Rgb(117, 7, 135),   // purple
    )

    override fun renderLine(y: Int, width: Int): Strip {
        val region = lastRegion ?: return Strip.EMPTY
        val height = region.height.coerceAtLeast(1)
        val stripeHeight = ((height + stripes.size - 1) / stripes.size).coerceAtLeast(1)
        val idx = (y / stripeHeight).coerceIn(0, stripes.lastIndex)
        return Strip.of(Segment(" ".repeat(width), Style(bgcolor = stripes[idx])))
    }
}

public fun main() {
    PrideApp().run()
}
