package tools.konsole.textual.widget

import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.dom.DOMNode

/**
 * Base class for UI elements. Mirrors Python textual's `Widget`.
 *
 * A Widget knows how to render itself within an allocated [Region] (provided by
 * the [tools.konsole.textual.compositor.Compositor]) and how to handle events
 * routed by the [tools.konsole.textual.message.MessagePump] superclass.
 *
 * The simplest widget overrides [render] to return a single [Renderable]:
 *
 * ```
 * class Hello : Widget() {
 *     override fun render(): Renderable = Text("Hello, world")
 * }
 * ```
 *
 * Subclasses with multiple child widgets override [compose] to yield them.
 */
public abstract class Widget(
    id: String? = null,
    classes: Set<String> = emptySet(),
) : DOMNode(id, classes) {

    /** Whether this widget can receive keyboard focus. */
    public open val canFocus: Boolean get() = false

    /** Currently focused? Set by [tools.konsole.textual.app.App.setFocus]. */
    public var hasFocus: Boolean = false
        internal set

    /** Pointer is hovering over this widget. Set by the App's MouseMove handler. */
    public var isHovered: Boolean = false
        internal set

    /** Pointer button is currently held down on this widget. Set on mouse-down, cleared on mouse-up. */
    public var isPressed: Boolean = false
        internal set

    /**
     * Whether this widget is disabled — does not receive focus, does not
     * fire bound actions on key/mouse events, and matches the `:disabled`
     * CSS pseudo-class. Mirrors textual's `Widget.disabled`.
     */
    public var disabled: Boolean = false

    /**
     * Active CSS pseudo-classes for this widget, computed from interaction
     * state every time the stylesheet is queried. Selectors like
     * `Button:hover` re-match per frame against this set, so styles update
     * as the user focuses / hovers / disables widgets.
     */
    override val activePseudoClasses: Set<String> get() = buildSet {
        if (hasFocus) add("focus")
        if (isHovered) add("hover")
        if (isPressed) add("active")
        if (disabled) add("disabled") else add("enabled")
    }

    /**
     * The screen-space region this widget was last placed at, set by the
     * [tools.konsole.textual.compositor.Compositor] during the most recent
     * render. `null` if the widget has never been placed.
     *
     * Widgets that need to size their content to their actual dimensions
     * (Sparkline, DataTable, Markdown viewport) can consult this rather than
     * synthesising an 80-wide console for [render].
     */
    public var lastRegion: tools.konsole.rich.geometry.Region? = null
        internal set

    /** Class-level bindings discovered by walking class hierarchy / `BINDINGS` companion fields. */
    public open val bindings: BindingsMap = BindingsMap()

    /**
     * Hint to the compositor about which overlay layer this widget belongs on
     * when added to a [tools.konsole.textual.compositor.Compositor]. `null`
     * means "use the caller-supplied layer or the default `BASE`". Widgets
     * that are inherently floating (Toast/Tooltip/popups) override this to
     * pin themselves to the right layer regardless of where they're placed.
     */
    public open val preferredLayer: tools.konsole.textual.compositor.Compositor.Layer? get() = null

    /**
     * Compose child widgets. Default: no children.
     * Override and yield via the [Sequence] builder:
     *
     * ```
     * override fun compose(): Sequence<Widget> = sequence {
     *     yield(Header())
     *     yield(MainContent())
     *     yield(Footer())
     * }
     * ```
     */
    public open fun compose(): Sequence<Widget> = emptySequence()

    /**
     * Render the widget's primary content. Return a [Renderable] (Text/Panel/Table/…).
     * Override for content-only widgets; if you need per-line control, override [renderLine] instead.
     */
    public open fun render(): Renderable = tools.konsole.rich.Text("")

    /**
     * Render this widget's content as a list of [Strip]s for the row range
     * `[startY, startY + count)`. The compositor calls this once per
     * placement; for [tools.konsole.textual.widget.Scrollable] widgets
     * [startY] reflects the current scroll offset.
     *
     * The default implementation renders [render] once and splits its
     * [Segment] stream by newline, fixing the O(N²) cost of calling the
     * legacy per-line [renderLine] N times for widgets that only produce
     * a single [Renderable]. Override when content is indexable per-row
     * (logs, tables) so rows outside the visible window aren't built.
     */
    public open fun renderStrips(width: Int, startY: Int, count: Int): List<Strip> {
        if (count <= 0) return emptyList()
        val flat = render().render(
            console = tools.konsole.rich.Console.string(width = width),
            options = tools.konsole.rich.RenderOptions(maxWidth = width),
        )
        val lines: MutableList<MutableList<Segment>> = mutableListOf(mutableListOf())
        for (seg in flat) {
            if (seg.text == "\n") lines.add(mutableListOf())
            else lines.last().add(seg)
        }
        val result = ArrayList<Strip>(count)
        for (i in 0 until count) {
            val row = lines.getOrNull(startY + i)
            result += if (row != null) Strip.of(row).adjustCellLength(width) else Strip.EMPTY
        }
        return result
    }

    /**
     * Render a single line at [y] (0 is the top of the widget's region) as a [Strip].
     * Convenience that delegates to [renderStrips]; the compositor uses
     * [renderStrips] directly so the full-content render is paid once per frame.
     */
    public open fun renderLine(y: Int, width: Int): Strip =
        renderStrips(width, y, 1).firstOrNull() ?: Strip.EMPTY

    /** Request a re-render. Default no-op; Phase 8 hooks this into the compositor's dirty list. */
    public open fun refresh() { /* Phase 8 */ }

    /**
     * Preferred ("natural") width of this widget when given [maxWidth] cells
     * to expand into. Used by [tools.konsole.textual.layouts.GridLayout]'s
     * `auto` column sizing to fit a track to its widest child.
     *
     * Default implementation renders to an in-memory [tools.konsole.rich.Console]
     * and reports the longest rendered line's cell width. Override for cheap
     * widget-specific estimates (e.g. a Label can return its text length
     * directly without going through the render pipeline).
     */
    public open fun naturalWidth(maxWidth: Int = 80): Int = try {
        val cw = maxWidth.coerceAtLeast(1)
        val rendered = render().render(
            console = tools.konsole.rich.Console.string(width = cw),
            options = tools.konsole.rich.RenderOptions(maxWidth = cw),
        )
        var maxLineWidth = 0
        var currentLine = 0
        for (seg in rendered) {
            for (ch in seg.text) {
                if (ch == '\n') { if (currentLine > maxLineWidth) maxLineWidth = currentLine; currentLine = 0 }
                else currentLine += 1
            }
        }
        if (currentLine > maxLineWidth) maxLineWidth = currentLine
        maxLineWidth.coerceIn(1, maxWidth)
    } catch (_: Throwable) {
        1
    }

    /**
     * Preferred ("natural") height of this widget when given [forWidth] cells
     * of horizontal space. Used by [tools.konsole.textual.layouts.GridLayout]'s
     * `auto` row sizing to fit a track to its tallest child at that width.
     */
    public open fun naturalHeight(forWidth: Int = 80): Int = try {
        val cw = forWidth.coerceAtLeast(1)
        val rendered = render().render(
            console = tools.konsole.rich.Console.string(width = cw),
            options = tools.konsole.rich.RenderOptions(maxWidth = cw),
        )
        var lines = 1
        for (seg in rendered) for (ch in seg.text) if (ch == '\n') lines += 1
        lines.coerceAtLeast(1)
    } catch (_: Throwable) {
        1
    }

    /** True if any of this widget's [bindings] match the [event]. */
    public fun hasBindingFor(event: tools.konsole.textual.events.Key): Boolean =
        bindings.match(event) != null
}
