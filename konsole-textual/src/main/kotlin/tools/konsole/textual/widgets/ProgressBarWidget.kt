package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.progress.ProgressBar as RichProgressBar
import tools.konsole.textual.widget.Widget

/**
 * Visual progress indicator widget — wraps rich's [RichProgressBar] (the full
 * pulse-animation-capable bar with half-cell boundaries, ported in Phase 4).
 *
 * Mirrors Python textual's `ProgressBar` *widget* (not to be confused with rich's
 * progress bar renderable — this is the higher-level wrapper).
 *
 * @param total total step count; `null` enables pulse animation.
 * @param progress current completed step count. Mutable so `advance()` works.
 * @param showBar render the bar.
 * @param showPercentage render `N%` after the bar.
 * @param showEta render time-remaining estimate. (Not implemented in Phase 9 — see Phase 9.5.)
 */
public open class ProgressBar(
    public var total: Double? = 100.0,
    public var progress: Double = 0.0,
    public val showBar: Boolean = true,
    public val showPercentage: Boolean = true,
    @Suppress("UNUSED_PARAMETER") public val showEta: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    /** Advance the progress by [amount]. Triggers a refresh. */
    public fun advance(amount: Double = 1.0) {
        progress += amount
        refresh()
    }

    /** Replace the progress count outright. */
    public fun update(progress: Double, total: Double? = this.total) {
        this.progress = progress
        this.total = total
        refresh()
    }

    override fun render(): Renderable = RichProgressBar(
        total = total,
        completed = progress,
        pulse = total == null,
    )
}
