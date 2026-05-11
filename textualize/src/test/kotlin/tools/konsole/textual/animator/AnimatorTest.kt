package tools.konsole.textual.animator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class AnimatorTest : StringSpec({

    "Easing.byName returns the named curve" {
        Easing.byName("linear") shouldBe Easing.Linear
        Easing.byName("out-cubic") shouldBe Easing.OutCubic
        Easing.byName("in-out-sine") shouldBe Easing.InOutSine
        Easing.byName("nonsense") shouldBe Easing.Linear   // unknown → linear
    }

    "Easing curves anchor at 0 and 1 (within fp tolerance)" {
        val eps = 1e-9
        listOf(
            Easing.Linear, Easing.InQuad, Easing.OutQuad, Easing.InOutQuad,
            Easing.InCubic, Easing.OutCubic, Easing.InOutCubic,
            Easing.InSine, Easing.OutSine, Easing.InOutSine,
            Easing.InExpo, Easing.OutExpo, Easing.InOutExpo,
            Easing.InBack, Easing.OutBack,
        ).forEach { e ->
            (kotlin.math.abs(e.ease(0.0)) < eps) shouldBe true
            (kotlin.math.abs(e.ease(1.0) - 1.0) < eps) shouldBe true
        }
    }

    "tween fires onValue with monotonically increasing values and lands on `to`" {
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val animator = Animator(scope)
            val values = mutableListOf<Double>()
            val job = animator.tween(from = 0.0, to = 1.0, durationMs = 100L, easing = Easing.Linear) { v ->
                synchronized(values) { values += v }
            }
            job.join()
            // Final value must hit the target exactly.
            values.last() shouldBe 1.0
            // We must have fired at least a few intermediate values.
            (values.size >= 3) shouldBe true
            // Linear easing keeps values monotonic.
            for (i in 1 until values.size) {
                (values[i] >= values[i - 1]) shouldBe true
            }
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        }
    }

    "tweenInt rounds each frame" {
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val animator = Animator(scope)
            val values = mutableListOf<Int>()
            val job = animator.tweenInt(from = 0, to = 100, durationMs = 80L, easing = Easing.Linear) { v ->
                synchronized(values) { values += v }
            }
            job.join()
            values.last() shouldBe 100
            // Every value should be a legal int in [0, 100].
            for (v in values) (v in 0..100) shouldBe true
        }
    }

    "animation can be cancelled mid-flight" {
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val animator = Animator(scope)
            var lastValue = -1.0
            val job = animator.tween(from = 0.0, to = 1.0, durationMs = 1000L) { lastValue = it }
            delay(20L)
            job.cancel()
            delay(50L)
            // We saw progress (not at start) but didn't reach the end.
            lastValue shouldNotBe -1.0
            lastValue shouldNotBe 1.0
        }
    }
})
