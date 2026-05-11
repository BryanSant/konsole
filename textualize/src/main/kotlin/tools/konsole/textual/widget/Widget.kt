package tools.konsole.textual.widget

import kotlinx.coroutines.Job
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
     *
     * Returns one of 16 shared, immutable sets keyed by the four boolean
     * flags — no allocation per read even though the CSS resolver may call
     * this for every widget on every frame.
     */
    override val activePseudoClasses: Set<String> get() {
        val key = (if (hasFocus) 1 else 0) or
                  (if (isHovered) 2 else 0) or
                  (if (isPressed) 4 else 0) or
                  (if (disabled) 8 else 0)
        return PSEUDO_CLASS_SETS[key]
    }

    /**
     * Single-shot timer that resets [isPressed] back to false after a Click
     * event. Kept as a field so a second click on the same widget can cancel
     * the prior timer rather than stack a parallel coroutine on the App's
     * root scope — see [tools.konsole.textual.app.App] click handling.
     */
    internal var pressedTimer: Job? = null

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
     * a single [Renderable]. If a subclass overrides [renderLine] but not
     * this method, the default detects that and dispatches per-row so the
     * existing override is honoured (back-compat for pre-renderStrips
     * widgets).
     *
     * Override directly for content with indexable per-row representation
     * (logs, tables) so rows outside the visible window aren't built.
     */
    public open fun renderStrips(width: Int, startY: Int, count: Int): List<Strip> {
        if (count <= 0) return emptyList()
        if (subclassOverridesRenderLine(this::class.java)) {
            val result = ArrayList<Strip>(count)
            for (i in 0 until count) result += renderLine(startY + i, width)
            return result
        }
        val lines = renderAllLines(width)
        val result = ArrayList<Strip>(count)
        for (i in 0 until count) {
            val row = lines.getOrNull(startY + i)
            result += if (row != null) Strip.of(row).adjustCellLength(width) else Strip.EMPTY
        }
        return result
    }

    /**
     * Render a single line at [y] (0 is the top of the widget's region) as a [Strip].
     *
     * The default impl renders [render] once and slices out row [y]. The
     * compositor uses [renderStrips] (not this), so the full-content
     * render is paid once per frame; direct callers of [renderLine] pay
     * it once per call.
     */
    public open fun renderLine(y: Int, width: Int): Strip {
        val lines = renderAllLines(width)
        val row = lines.getOrNull(y) ?: return Strip.EMPTY
        return Strip.of(row).adjustCellLength(width)
    }

    private fun renderAllLines(width: Int): List<List<Segment>> {
        val flat = render().render(
            console = tools.konsole.rich.Console.string(width = width),
            options = tools.konsole.rich.RenderOptions(maxWidth = width),
        )
        val lines: MutableList<MutableList<Segment>> = mutableListOf(mutableListOf())
        for (seg in flat) {
            if (seg.text == "\n") lines.add(mutableListOf())
            else lines.last().add(seg)
        }
        return lines
    }

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

    private companion object {
        // 4 boolean flags → 16 distinct {focus/hover/active/disabled/enabled} sets.
        private val PSEUDO_CLASS_SETS: Array<Set<String>> = Array(16) { key ->
            buildSet {
                if (key and 1 != 0) add("focus")
                if (key and 2 != 0) add("hover")
                if (key and 4 != 0) add("active")
                if (key and 8 != 0) add("disabled") else add("enabled")
            }
        }

        // Cached per concrete class: does it override renderLine? Lets the
        // default renderStrips honour pre-renderStrips subclasses that draw
        // their own per-row content. Look-up is O(1) after first hit.
        private val RENDER_LINE_OVERRIDES: java.util.concurrent.ConcurrentHashMap<Class<*>, Boolean> =
            java.util.concurrent.ConcurrentHashMap()

        private fun subclassOverridesRenderLine(cls: Class<*>): Boolean = RENDER_LINE_OVERRIDES.getOrPut(cls) {
            try {
                val m = cls.getMethod("renderLine", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                m.declaringClass != Widget::class.java
            } catch (_: Throwable) {
                false
            }
        }
    }
}
