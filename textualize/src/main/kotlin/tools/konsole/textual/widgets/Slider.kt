package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/**
 * Numeric slider — drag-to-set within `[min, max]`. Mirrors the
 * textual community widget. Arrow keys nudge by [step]; Home / End jump
 * to the bounds; clicking on the track jumps the handle to that position.
 *
 * Posts a [Changed] message whenever [value] changes.
 *
 * Single-row layout: `[━━━━╸━━━━━]` with the filled portion on the left
 * and a handle marker (`●` by default) at the current position.
 *
 * @param value initial value, clamped into `[min, max]`.
 * @param min lower bound (inclusive).
 * @param max upper bound (inclusive). Must be > [min].
 * @param step nudge size for Arrow/Page keys.
 */
public open class Slider(
    value: Double = 0.0,
    public val min: Double = 0.0,
    public val max: Double = 100.0,
    public val step: Double = 1.0,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    init { require(max > min) { "Slider max ($max) must be > min ($min)" } }

    public var value: Double = value.coerceIn(min, max)
        private set

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("left", "decrement", show = false),
            Binding("down", "decrement", show = false),
            Binding("right", "increment", show = false),
            Binding("up", "increment", show = false),
            Binding("home", "minimum", show = false),
            Binding("end", "maximum", show = false),
            Binding("pageup", "decrement_large", show = false),
            Binding("pagedown", "increment_large", show = false),
        ),
    )

    @Suppress("unused") public fun action_decrement() { setValue(value - step) }
    @Suppress("unused") public fun action_increment() { setValue(value + step) }
    @Suppress("unused") public fun action_decrement_large() { setValue(value - step * 10.0) }
    @Suppress("unused") public fun action_increment_large() { setValue(value + step * 10.0) }
    @Suppress("unused") public fun action_minimum() { setValue(min) }
    @Suppress("unused") public fun action_maximum() { setValue(max) }

    /** Set the value (clamped to `[min, max]`); fires [Changed] only when it actually moves. */
    public fun setValue(newValue: Double) {
        val clamped = newValue.coerceIn(min, max)
        if (clamped == value) return
        value = clamped
        post(Changed(this, value))
        refresh()
    }

    /** Normalized position in `[0, 1]`. */
    public val ratio: Double get() = ((value - min) / (max - min)).coerceIn(0.0, 1.0)

    override suspend fun onEvent(event: tools.konsole.textual.events.Event) {
        if (event is tools.konsole.textual.events.Click) {
            // Jump the handle to the clicked column inside our region.
            val region = lastRegion ?: return
            val relX = (event.x - region.x).coerceIn(0, (region.width - 1).coerceAtLeast(0))
            val newRatio = if (region.width > 1) relX.toDouble() / (region.width - 1) else 0.0
            setValue(min + (max - min) * newRatio)
            event.stop()
        }
    }

    override fun render(): Renderable = Text("")  // we draw the line ourselves

    override fun renderLine(y: Int, width: Int): Strip {
        if (y != 0) return Strip.EMPTY
        if (width <= 0) return Strip.EMPTY
        val handleCol = ((width - 1) * ratio).toInt().coerceIn(0, width - 1)
        val filledStyle = Style(color = if (hasFocus) Color.Cyan else Color.Blue, bold = true)
        val emptyStyle = Style(color = Color.DarkGrey)
        val handleStyle = Style(color = if (hasFocus) Color.Yellow else Color.White, bold = true)

        val sb = StringBuilder()
        val segments = mutableListOf<Segment>()
        if (handleCol > 0) segments += Segment("━".repeat(handleCol), filledStyle)
        segments += Segment("●", handleStyle)
        if (handleCol < width - 1) segments += Segment("━".repeat(width - 1 - handleCol), emptyStyle)
        return Strip.of(segments)
    }

    /** Posted when [value] changes. */
    public data class Changed(val slider: Slider, val value: Double) : Message()
}
