package tools.konsole.textual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.textual.signal.Signal

class SignalTest : StringSpec({

    "publish fires subscribers in subscription order" {
        val sig = Signal<Int>()
        val seen = mutableListOf<Int>()
        sig.subscribe { seen += it * 10 }
        sig.subscribe { seen += it * 100 }
        sig.publish(5)
        seen shouldBe listOf(50, 500)
    }

    "disconnect stops a single subscriber" {
        val sig = Signal<String>()
        var captured: String? = null
        val sub = sig.subscribe { captured = it }
        sub.disconnect()
        sig.publish("hello")
        captured shouldBe null
    }

    "a throwing subscriber doesn't block other subscribers" {
        val sig = Signal<Unit>()
        var afterFired = false
        sig.subscribe { error("boom") }
        sig.subscribe { afterFired = true }
        sig.publish(Unit)
        afterFired shouldBe true
    }

    "clear removes all subscribers" {
        val sig = Signal<Int>()
        var seen = 0
        sig.subscribe { seen += it }
        sig.clear()
        sig.publish(1)
        seen shouldBe 0
    }
})
