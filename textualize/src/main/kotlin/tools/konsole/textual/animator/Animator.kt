package tools.konsole.textual.animator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

/**
 * Easing function: `progress in 0..1 → eased in 0..1`.
 * Mirrors a subset of textual's `_animator.py` easing curves.
 */
public fun interface Easing {
    public fun ease(t: Double): Double

    public companion object {
        public val Linear: Easing = Easing { it }
        public val InQuad: Easing = Easing { it * it }
        public val OutQuad: Easing = Easing { 1.0 - (1.0 - it) * (1.0 - it) }
        public val InOutQuad: Easing = Easing { if (it < 0.5) 2.0 * it * it else 1.0 - 2.0 * (1.0 - it) * (1.0 - it) }
        public val InOutSine: Easing = Easing { -(cos(PI * it) - 1.0) / 2.0 }

        /** Look up a named easing curve (lowercase, hyphenated). Returns [Linear] for unknown. */
        public fun byName(name: String): Easing = when (name) {
            "linear" -> Linear
            "in-quad" -> InQuad
            "out-quad" -> OutQuad
            "in-out-quad" -> InOutQuad
            "in-out-sine" -> InOutSine
            else -> Linear
        }
    }
}

/**
 * Single animation tracking a `Double`-valued property. Calls [onValue] each frame
 * with the eased current value. Completes when `duration` elapses.
 *
 * Use [Animator.animate] to schedule one inside a [CoroutineScope].
 */
public class Animation(
    public val from: Double,
    public val to: Double,
    public val durationMs: Long,
    public val easing: Easing = Easing.Linear,
    public val onValue: (Double) -> Unit,
    public val onDone: () -> Unit = {},
)

/**
 * Drives [Animation]s on a [CoroutineScope]. Mirrors textual's `Animator`.
 *
 * Each frame ticks at ~60 Hz (16 ms `delay`). Active animations are stored in a
 * list and cleared as they complete. New animations can be added at any time.
 */
public class Animator(public val scope: CoroutineScope, public val fps: Int = 60) {

    private val frameMs: Long get() = (1000L / fps).coerceAtLeast(1L)

    public fun animate(anim: Animation): Job = scope.launch {
        val start = System.nanoTime()
        val end = start + anim.durationMs * 1_000_000L
        while (true) {
            val now = System.nanoTime()
            if (now >= end) {
                anim.onValue(anim.to)
                anim.onDone()
                break
            }
            val progress = (now - start).toDouble() / (anim.durationMs * 1_000_000L)
            val eased = anim.easing.ease(progress.coerceIn(0.0, 1.0))
            val current = anim.from + (anim.to - anim.from) * eased
            anim.onValue(current)
            delay(frameMs)
        }
    }
}
