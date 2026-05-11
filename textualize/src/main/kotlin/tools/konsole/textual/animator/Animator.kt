package tools.konsole.textual.animator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Easing function: `progress in 0..1 → eased in 0..1`.
 * Mirrors a superset of textual's `_animator.py` easing curves plus the
 * classic Robert Penner / `css-easings.net` cubic / exponential / back /
 * elastic variants.
 */
public fun interface Easing {
    public fun ease(t: Double): Double

    public companion object {
        public val Linear: Easing = Easing { it }

        // Quadratic
        public val InQuad: Easing = Easing { it * it }
        public val OutQuad: Easing = Easing { 1.0 - (1.0 - it) * (1.0 - it) }
        public val InOutQuad: Easing = Easing { if (it < 0.5) 2.0 * it * it else 1.0 - 2.0 * (1.0 - it) * (1.0 - it) }

        // Cubic
        public val InCubic: Easing = Easing { it * it * it }
        public val OutCubic: Easing = Easing { 1.0 - (1.0 - it).pow(3.0) }
        public val InOutCubic: Easing = Easing { if (it < 0.5) 4.0 * it * it * it else 1.0 - (-2.0 * it + 2.0).pow(3.0) / 2.0 }

        // Sine
        public val InSine: Easing = Easing { 1.0 - cos(it * PI / 2.0) }
        public val OutSine: Easing = Easing { sin(it * PI / 2.0) }
        public val InOutSine: Easing = Easing { -(cos(PI * it) - 1.0) / 2.0 }

        // Exponential
        public val InExpo: Easing = Easing { if (it == 0.0) 0.0 else 2.0.pow(10.0 * it - 10.0) }
        public val OutExpo: Easing = Easing { if (it == 1.0) 1.0 else 1.0 - 2.0.pow(-10.0 * it) }
        public val InOutExpo: Easing = Easing {
            when {
                it == 0.0 -> 0.0
                it == 1.0 -> 1.0
                it < 0.5 -> 2.0.pow(20.0 * it - 10.0) / 2.0
                else -> (2.0 - 2.0.pow(-20.0 * it + 10.0)) / 2.0
            }
        }

        // Back — overshoots slightly past the target before settling.
        public val InBack: Easing = Easing { val c1 = 1.70158; val c3 = c1 + 1.0; c3 * it * it * it - c1 * it * it }
        public val OutBack: Easing = Easing {
            val c1 = 1.70158; val c3 = c1 + 1.0
            1.0 + c3 * (it - 1.0).pow(3.0) + c1 * (it - 1.0).pow(2.0)
        }

        // Elastic — bounces around the target before settling.
        public val OutElastic: Easing = Easing {
            val c4 = 2.0 * PI / 3.0
            when {
                it == 0.0 -> 0.0
                it == 1.0 -> 1.0
                else -> 2.0.pow(-10.0 * it) * sin((it * 10.0 - 0.75) * c4) + 1.0
            }
        }

        /** Look up a named easing curve (lowercase, hyphenated). Returns [Linear] for unknown. */
        public fun byName(name: String): Easing = when (name.lowercase()) {
            "linear" -> Linear
            "in-quad" -> InQuad
            "out-quad" -> OutQuad
            "in-out-quad" -> InOutQuad
            "in-cubic" -> InCubic
            "out-cubic" -> OutCubic
            "in-out-cubic" -> InOutCubic
            "in-sine" -> InSine
            "out-sine" -> OutSine
            "in-out-sine" -> InOutSine
            "in-expo" -> InExpo
            "out-expo" -> OutExpo
            "in-out-expo" -> InOutExpo
            "in-back" -> InBack
            "out-back" -> OutBack
            "out-elastic" -> OutElastic
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
 *
 * Typical use from inside an `App`:
 *
 * ```
 * val anim = app.animator
 * anim.tween(from = 0.0, to = 1.0, durationMs = 250L) { progress ->
 *     widget.opacity = progress
 *     requestRefresh()
 * }
 * ```
 */
public class Animator(public val scope: CoroutineScope, public val fps: Int = 60) {

    private val frameMs: Long get() = (1000L / fps).coerceAtLeast(1L)

    /** Schedule a raw [Animation]. Returns the [Job] for cancellation. */
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

    /**
     * Tween a `Double` value from [from] to [to] over [durationMs], invoking
     * [onValue] every frame with the current eased value. Convenience wrapper
     * over [animate].
     */
    public fun tween(
        from: Double,
        to: Double,
        durationMs: Long,
        easing: Easing = Easing.OutCubic,
        onDone: () -> Unit = {},
        onValue: (Double) -> Unit,
    ): Job = animate(Animation(from = from, to = to, durationMs = durationMs, easing = easing, onValue = onValue, onDone = onDone))

    /**
     * Tween an `Int` value — handy for animating cell positions, widths, etc.
     * Internally tweens the underlying Double and rounds each frame.
     */
    public fun tweenInt(
        from: Int,
        to: Int,
        durationMs: Long,
        easing: Easing = Easing.OutCubic,
        onDone: () -> Unit = {},
        onValue: (Int) -> Unit,
    ): Job = tween(
        from = from.toDouble(),
        to = to.toDouble(),
        durationMs = durationMs,
        easing = easing,
        onDone = onDone,
        onValue = { onValue(it.toInt()) },
    )
}
