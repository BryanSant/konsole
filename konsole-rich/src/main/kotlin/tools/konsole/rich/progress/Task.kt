package tools.konsole.rich.progress

import tools.konsole.rich.Renderable

/**
 * A unit of work tracked by [Progress]. Values are mutable: [completed], [total], [description]
 * change over time as the work makes progress.
 */
public class Task(
    public val id: Int,
    public var description: Renderable,
    public var total: Double? = 100.0,
    public var completed: Double = 0.0,
    public var visible: Boolean = true,
    public var fields: MutableMap<String, Any?> = mutableMapOf(),
) {
    /** Time the task was first started (nanos). */
    public var startTime: Long? = null
        internal set

    /** Time the task finished (nanos). null if still running. */
    public var stopTime: Long? = null
        internal set

    public val started: Boolean get() = startTime != null
    public val finished: Boolean get() = stopTime != null

    /** Percentage 0..100, or null if total is null/0. */
    public val percentage: Double?
        get() {
            val t = total ?: return null
            if (t <= 0.0) return null
            return ((completed / t) * 100.0).coerceIn(0.0, 100.0)
        }

    /** Elapsed seconds since [startTime] (or since [stopTime] if finished). */
    public val elapsed: Double?
        get() {
            val s = startTime ?: return null
            val end = stopTime ?: System.nanoTime()
            return (end - s) / 1_000_000_000.0
        }

    /** Best-effort speed (units per second). */
    public val speed: Double?
        get() {
            val e = elapsed ?: return null
            if (e <= 0.0 || completed <= 0.0) return null
            return completed / e
        }

    /** Estimated seconds remaining. */
    public val timeRemaining: Double?
        get() {
            val s = speed ?: return null
            val t = total ?: return null
            val left = t - completed
            if (left <= 0.0) return 0.0
            return left / s
        }
}
