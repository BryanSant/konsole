package tools.konsole.rich

import tools.konsole.core.style.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StyleMergeTest : StringSpec({

    "right-side non-null wins" {
        val a = Style(color = Color.Red, bold = true)
        val b = Style(color = Color.Blue)
        (a + b) shouldBe Style(color = Color.Blue, bold = true)
    }

    "null fields fall through" {
        val a = Style(bold = true, italic = true)
        val b = Style.NULL
        (a + b) shouldBe a
        (b + a) shouldBe a
    }

    "false survives merge" {
        // explicitly off should not be overridden by null
        val a = Style(bold = true)
        val b = Style(bold = false)
        (a + b) shouldBe Style(bold = false)
    }

    "meta merges" {
        val a = Style(meta = mapOf("k1" to 1))
        val b = Style(meta = mapOf("k2" to 2))
        (a + b).meta shouldBe mapOf("k1" to 1, "k2" to 2)
    }

    "isNull only true for default" {
        Style.NULL.isNull shouldBe true
        Style(bold = true).isNull shouldBe false
    }
})
