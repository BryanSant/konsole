package tools.konsole.rich.progress

import tools.konsole.rich.Text
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TaskTest : StringSpec({

    "percentage is null when total is null" {
        val t = Task(0, Text("x"), total = null)
        t.percentage shouldBe null
    }

    "percentage clamps to 0..100" {
        val t = Task(0, Text("x"), total = 100.0, completed = 50.0)
        t.percentage shouldBe 50.0

        val over = Task(1, Text("x"), total = 100.0, completed = 200.0)
        over.percentage shouldBe 100.0

        val neg = Task(2, Text("x"), total = 100.0, completed = -10.0)
        neg.percentage shouldBe 0.0
    }

    "elapsed null until started" {
        val t = Task(0, Text("x"))
        t.elapsed shouldBe null
        t.startTime = System.nanoTime() - 1_000_000_000
        val e = t.elapsed
        check(e != null && e >= 1.0) { "expected elapsed >= 1s, got $e" }
    }

    "speed null with no completion" {
        val t = Task(0, Text("x"))
        t.startTime = System.nanoTime() - 2_000_000_000
        t.speed shouldBe null
    }

    "time remaining computed from speed" {
        val t = Task(0, Text("x"), total = 100.0, completed = 50.0)
        t.startTime = System.nanoTime() - 5_000_000_000  // 5 seconds elapsed
        // speed = 50/5 = 10/sec; remaining = 50; time = 50/10 = 5s
        val tr = t.timeRemaining
        check(tr != null && tr in 4.0..6.0) { "expected ~5s remaining, got $tr" }
    }
})
