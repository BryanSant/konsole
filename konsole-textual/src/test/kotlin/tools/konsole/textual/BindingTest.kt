package tools.konsole.textual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyModifiers
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.events.Key

class BindingTest : StringSpec({

    "BindingsMap matches keys by their textual key string" {
        val map = bindings("q" to "quit", "ctrl+c" to "cancel")
        map.match(Key(KeyCode.Char('q')))?.action shouldBe "quit"
        map.match(Key(KeyCode.Char('c'), KeyModifiers.CONTROL))?.action shouldBe "cancel"
    }

    "BindingsMap returns null for unbound keys" {
        val map = bindings("q" to "quit")
        map.match(Key(KeyCode.Char('x'))) shouldBe null
    }

    "priority bindings beat regular bindings on same key" {
        val map = BindingsMap()
        map.add(Binding(key = "q", action = "regular"))
        map.add(Binding(key = "q", action = "priority", priority = true))
        map.match(Key(KeyCode.Char('q')))?.action shouldBe "priority"
    }
})
