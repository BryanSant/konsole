package tools.konsole.rich

import tools.konsole.core.style.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ThemeTest : StringSpec({

    "lookup direct" {
        val t = Theme(mapOf("k" to Style(color = Color.Red)))
        t["k"] shouldBe Style(color = Color.Red)
        t["missing"] shouldBe null
    }

    "parent chain" {
        val parent = Theme(mapOf("a" to Style(color = Color.Red)))
        val child = Theme(mapOf("b" to Style(color = Color.Blue)), parent = parent)
        child["a"] shouldBe Style(color = Color.Red)
        child["b"] shouldBe Style(color = Color.Blue)
    }

    "child overrides parent" {
        val parent = Theme(mapOf("k" to Style(color = Color.Red)))
        val child = Theme(mapOf("k" to Style(color = Color.Blue)), parent = parent)
        child["k"] shouldBe Style(color = Color.Blue)
    }

    "plus layers overrides" {
        val parent = Theme(mapOf("a" to Style(color = Color.Red)))
        val child = parent + mapOf("b" to Style(color = Color.Green))
        child["a"] shouldBe Style(color = Color.Red)
        child["b"] shouldBe Style(color = Color.Green)
    }

    "DEFAULT contains semantic styles" {
        Theme.DEFAULT["error"] shouldBe Style(color = Color.Red, bold = true)
        Theme.DEFAULT["info"] shouldBe Style(color = Color.Cyan)
    }
})
