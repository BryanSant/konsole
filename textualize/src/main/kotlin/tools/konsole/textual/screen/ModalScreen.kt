package tools.konsole.textual.screen

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.widget.Widget

/**
 * A semi-transparent overlay screen. Mirrors Python textual's `ModalScreen` —
 * draws a dimming backdrop over the lower screens, hosts focusable widgets
 * on top, and intercepts all input. Escape dismisses by default.
 *
 * Push via `app.pushScreen(myModal)`; pop with [dismiss] or the bound
 * `escape` action. The modal returns a value to its caller via [onResult]
 * (or via the `result` accessor after the dismissal).
 *
 * Subclass and override [compose] to declare the modal's content. The
 * built-in backdrop is rendered by the base class — your widgets render
 * on top of it.
 */
public abstract class ModalScreen<T>(
    /**
     * If true, the modal renders a dimming backdrop across the full
     * viewport before drawing its content. Disable for transparent
     * tooltip-style overlays.
     */
    public val showBackdrop: Boolean = true,
    /**
     * Background style for the backdrop fill. Defaults to a dim dark grey.
     */
    public val backdropStyle: Style = Style(bgcolor = Color.Rgb(0x10, 0x10, 0x10), dim = true),
    /**
     * If true, pressing Escape calls [dismiss] with `null`. Disable for
     * modals that must be resolved via their own buttons.
     */
    public val dismissOnEscape: Boolean = true,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Screen(id, classes) {

    private var resultValue: T? = null

    /** Callback fired when the modal is dismissed. Set via [onResult]. */
    private var resultHandler: ((T?) -> Unit)? = null

    override val bindings: BindingsMap = BindingsMap(
        if (dismissOnEscape) listOf(Binding("escape", "dismiss", show = false)) else emptyList(),
    )

    @Suppress("unused") public fun action_dismiss() { dismiss(null) }

    /**
     * Resolve the modal with [value] and pop it off the screen stack. The
     * [onResult] callback (if set) is invoked synchronously before the pop.
     */
    public open fun dismiss(value: T?) {
        resultValue = value
        resultHandler?.invoke(value)
        // Find the App ancestor and pop ourselves off its screen stack.
        var node = parent
        while (node != null && node !is tools.konsole.textual.app.App) node = node.parent
        if (node is tools.konsole.textual.app.App) node.popScreen()
    }

    /** Register a callback invoked when [dismiss] is called. */
    public fun onResult(handler: (T?) -> Unit) {
        resultHandler = handler
    }

    /** Last value passed to [dismiss], or `null` if not dismissed yet. */
    public val result: T? get() = resultValue

    /**
     * Render the backdrop. Subclasses' content (returned from [compose])
     * is rendered by the compositor over this base.
     */
    override fun renderStrips(width: Int, startY: Int, count: Int): List<Strip> {
        if (count <= 0) return emptyList()
        val row: Strip = if (showBackdrop) Strip.of(Segment(" ".repeat(width), backdropStyle)) else Strip.EMPTY
        return List(count) { row }
    }

    override fun render(): Renderable = Text("")  // children render themselves
}
