package tools.konsole.rich

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LRUCacheTest : StringSpec({

    "stores and retrieves values" {
        val c = LRUCache<String, Int>(capacity = 3)
        c["a"] = 1
        c["b"] = 2
        c["a"] shouldBe 1
        c["b"] shouldBe 2
        c["missing"] shouldBe null
    }

    "evicts least recently used at capacity" {
        val c = LRUCache<String, Int>(capacity = 2)
        c["a"] = 1
        c["b"] = 2
        c["a"]                  // touch a → b is now LRU
        c["c"] = 3              // evict b
        c["a"] shouldBe 1
        c["b"] shouldBe null
        c["c"] shouldBe 3
    }

    "getOrPut populates on miss" {
        val c = LRUCache<String, Int>(capacity = 2)
        var calls = 0
        val v = c.getOrPut("x") { calls++; 42 }
        v shouldBe 42
        c.getOrPut("x") { calls++; 99 }   // hit, default not invoked
        calls shouldBe 1
    }

    "FIFOCache evicts in insertion order regardless of access" {
        val c = FIFOCache<String, Int>(capacity = 2)
        c["a"] = 1
        c["b"] = 2
        c["a"]                  // FIFO ignores access
        c["c"] = 3              // evicts a (oldest)
        c["a"] shouldBe null
        c["b"] shouldBe 2
        c["c"] shouldBe 3
    }

    "rejects non-positive capacity" {
        try {
            LRUCache<String, Int>(0)
            error("expected IAE")
        } catch (_: IllegalArgumentException) { /* ok */ }
    }
})
