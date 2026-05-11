package tools.konsole.textual.widget

/**
 * Mixin interface for widgets that maintain scroll position. Mirrors Python
 * textual's `ScrollView`/`Scrollable` semantics — the abstraction most widget
 * ports get wrong (per the design plan's call-out), so it lives in its own file.
 *
 * A scrollable widget tracks `(scrollX, scrollY)` in cells. Subclasses override
 * [contentWidth] / [contentHeight] to declare the total scrollable extent and
 * call [scrollTo] / [scrollBy] / [scrollHome] / [scrollEnd] to navigate.
 *
 * The compositor is expected to clip the widget's rendered output to its
 * region after offsetting by `(scrollX, scrollY)`. Phase 9 ships the protocol;
 * the compositor wiring lands in Phase 9.5 with `ListView` and `RichLog`.
 */
public interface Scrollable {

    /** Total content width in cells. */
    public val contentWidth: Int

    /** Total content height in cells. */
    public val contentHeight: Int

    /** Current horizontal scroll offset in cells (>= 0). */
    public var scrollX: Int

    /** Current vertical scroll offset in cells (>= 0). */
    public var scrollY: Int

    /** Scroll to an absolute position, clamping at the content edges. */
    public fun scrollTo(x: Int = scrollX, y: Int = scrollY) {
        scrollX = x.coerceIn(0, (contentWidth - 1).coerceAtLeast(0))
        scrollY = y.coerceIn(0, (contentHeight - 1).coerceAtLeast(0))
    }

    /** Scroll by a relative delta. */
    public fun scrollBy(dx: Int = 0, dy: Int = 0) {
        scrollTo(scrollX + dx, scrollY + dy)
    }

    public fun scrollHome() { scrollTo(0, 0) }

    public fun scrollEnd() { scrollTo(0, (contentHeight - 1).coerceAtLeast(0)) }

    public fun scrollPageUp(viewportHeight: Int) { scrollBy(dy = -viewportHeight) }

    public fun scrollPageDown(viewportHeight: Int) { scrollBy(dy = viewportHeight) }
}
