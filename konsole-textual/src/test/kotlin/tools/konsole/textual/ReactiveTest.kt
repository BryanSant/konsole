package tools.konsole.textual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.textual.reactive.Reactable
import tools.konsole.textual.reactive.reactive

private class Counter : Reactable {
    var count: Int by reactive(0)
    var watcherCallCount: Int = 0
    var lastOldValue: Int = -1
    var lastNewValue: Int = -1

    @Suppress("unused")
    fun watch_count(old: Int, new: Int) {
        watcherCallCount += 1
        lastOldValue = old
        lastNewValue = new
    }
}

private class Validating : Reactable {
    var value: Int by reactive(0)

    @Suppress("unused")
    fun validate_value(@Suppress("UNUSED_PARAMETER") old: Int, new: Int): Int = new.coerceIn(0, 100)
}

class ReactiveTest : StringSpec({

    "reactive get/set works as a property" {
        val c = Counter()
        c.count shouldBe 0
        c.count = 42
        c.count shouldBe 42
    }

    "watch_<name>(old, new) fires on change" {
        val c = Counter()
        c.count = 1
        c.count = 2
        c.watcherCallCount shouldBe 2
        c.lastOldValue shouldBe 1
        c.lastNewValue shouldBe 2
    }

    "watch_<name> does NOT fire when value is unchanged" {
        val c = Counter()
        c.count = 1
        val before = c.watcherCallCount
        c.count = 1
        c.watcherCallCount shouldBe before
    }

    "validate_<name> coerces incoming values" {
        val v = Validating()
        v.value = -5
        v.value shouldBe 0
        v.value = 999
        v.value shouldBe 100
        v.value = 42
        v.value shouldBe 42
    }
})
