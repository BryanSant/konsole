package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.animator.Easing
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
 * Animator showcase. Press a digit 1–8 to fire an animation using a
 * different easing curve; the bar slides across the screen so you can
 * compare the curves side by side.
 *
 *   ./demo.sh Animation
 *
 * `q` or Ctrl+C to quit.
 */
public class AnimationDemo(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val bars = listOf(
        BarRow("linear",       Easing.Linear),
        BarRow("out-quad",     Easing.OutQuad),
        BarRow("out-cubic",    Easing.OutCubic),
        BarRow("out-sine",     Easing.OutSine),
        BarRow("out-expo",     Easing.OutExpo),
        BarRow("out-back",     Easing.OutBack),
        BarRow("out-elastic",  Easing.OutElastic),
        BarRow("in-out-cubic", Easing.InOutCubic),
    )

    override val bindings = bindings(
        "q" to "quit",
        "ctrl+c" to "quit",
        "space" to "fire_all",
    )

    @Suppress("unused") public fun action_fire_all() { fireAll() }

    private fun fireAll() {
        for (bar in bars) {
            animator.tween(
                from = 0.0,
                to = 1.0,
                durationMs = 1500L,
                easing = bar.easing,
            ) { progress ->
                bar.progress = progress
                requestRefresh()
            }
        }
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = buildList {
                add(Header(title = "Animator: press SPACE to fire all curves at once"))
                addAll(bars)
                add(Footer(this@AnimationDemo.bindings))
            },
            heights = buildList {
                add(Scalar(1.0, LengthUnit.Cells))                   // header
                for (b in bars) add(Scalar(1.0, LengthUnit.Fraction)) // bars share the body
                add(Scalar(1.0, LengthUnit.Cells))                   // footer
            },
        )
    )

    override fun start() {
        super.start()
        fireAll()
    }
}

private class BarRow(
    private val labelText: String,
    val easing: Easing,
) : Widget() {

    /** Animation progress 0..1 — drives the bar's visible width. */
    @Volatile var progress: Double = 0.0

    override fun render(): Renderable {
        val t = Text()
        t.append("${labelText.padEnd(14)} ", Style(color = Color.DarkGrey))
        return t
    }

    override fun renderLine(y: Int, width: Int): Strip {
        if (y != 0) return Strip.EMPTY
        val labelCols = 15
        val barCols = (width - labelCols).coerceAtLeast(1)
        val filled = ((progress.coerceIn(0.0, 1.0) * barCols).toInt()).coerceIn(0, barCols)
        val barStyle = Style(bgcolor = Color.Rgb(0x40, 0x90, 0xff))
        val emptyStyle = Style(bgcolor = Color.Rgb(0x20, 0x20, 0x20))
        return Strip.of(
            Segment(labelText.padEnd(labelCols), Style(color = Color.White)),
            Segment(" ".repeat(filled), barStyle),
            Segment(" ".repeat((barCols - filled).coerceAtLeast(0)), emptyStyle),
        )
    }
}

public fun main() {
    AnimationDemo().run()
}
