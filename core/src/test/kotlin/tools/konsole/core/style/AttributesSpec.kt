package tools.konsole.core.style

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll

class AttributesSpec : StringSpec({

    "NONE contains no attribute" {
        Attributes.NONE.isEmpty() shouldBe true
        for (a in Attribute.entries) (a in Attributes.NONE) shouldBe false
    }

    "+ adds an attribute idempotently" {
        val s = Attributes.NONE + Attribute.Bold + Attribute.Bold
        (Attribute.Bold in s) shouldBe true
        s.iterator().asSequence().toList() shouldContainExactly listOf(Attribute.Bold)
    }

    "- removes an attribute" {
        val s = Attributes.of(Attribute.Bold, Attribute.Italic) - Attribute.Bold
        (Attribute.Bold in s) shouldBe false
        (Attribute.Italic in s) shouldBe true
    }

    "Attributes union (+) is associative and identity-respecting" {
        checkAll(Arb.enum<Attribute>(), Arb.enum<Attribute>(), Arb.enum<Attribute>()) { a, b, c ->
            val left = (Attributes.of(a) + b) + c
            val right = Attributes.of(a) + (Attributes.of(b) + c)
            left shouldBe right
            (Attributes.NONE + a) shouldBe Attributes.of(a)
        }
    }

    "iteration is in enum-declaration order" {
        val s = Attributes.of(Attribute.Italic, Attribute.Bold, Attribute.Underlined)
        s.iterator().asSequence().toList() shouldContainExactly listOf(Attribute.Bold, Attribute.Italic, Attribute.Underlined)
    }
})
