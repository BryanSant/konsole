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
     * Render a single line at [y] (0 is the top of the widget's region) as a [Strip].
     * Default implementation calls [render] and slices its [Segment] stream into lines.
     */
    public open fun renderLine(y: Int, width: Int): Strip {
        // Lazy/uncached default — a real implementation in Phase 9 will memoize the rendered lines.
        val segments = mutableListOf<Segment>()
        val flat = render().render(
            console = tools.konsole.rich.Console.string(width = width),
            options = tools.konsole.rich.RenderOptions(maxWidth = width),
        )
        for (seg in flat) segments += seg
        // Split by newline segments
        val lines: MutableList<MutableList<Segment>> = mutableListOf(mutableListOf<Segment>())
        for (seg in segments) {
            if (seg.text == "\n") lines.add(mutableListOf())
            else lines.last().add(seg)
        }
        val line = lines.getOrNull(y) ?: return Strip.EMPTY
        return Strip.of(line).adjustCellLength(width)
    }

    /** Request a re-render. Default no-op; Phase 8 hooks this into the compositor's dirty list. */
    public open fun refresh() { /* Phase 8 */ }

    /** True if any of this widget's [bindings] match the [event]. */
    public fun hasBindingFor(event: tools.konsole.textual.events.Key): Boolean =
        bindings.match(event) != null
}
