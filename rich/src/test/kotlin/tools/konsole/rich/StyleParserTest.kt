package tools.konsole.rich

import tools.konsole.core.style.Color
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StyleParserTest : StringSpec({

    "empty string parses to NULL" {
        Style.parse("") shouldBe Style.NULL
        Style.parse("   ") shouldBe Style.NULL
    }

    "single attribute" {
        Style.parse("bold") shouldBe Style(bold = true)
        Style.parse("italic") shouldBe Style(italic = true)
        Style.parse("dim") shouldBe Style(dim = true)
    }

    "negation" {
        Style.parse("not bold") shouldBe Style(bold = false)
        Style.parse("not italic") shouldBe Style(italic = false)
    }

    "single named color" {
        Style.parse("red") shouldBe Style(color = Color.Red)
    }

    "color and attribute" {
        Style.parse("bold red") shouldBe Style(bold = true, color = Color.Red)
        Style.parse("red bold") shouldBe Style(color = Color.Red, bold = true)
    }

    "background via 'on'" {
        Style.parse("red on white") shouldBe Style(color = Color.Red, bgcolor = Color.White)
    }

    "compound" {
        val s = Style.parse("bold red on white")
        s shouldBe Style(bold = true, color = Color.Red, bgcolor = Color.White)
    }

    "link=" {
        Style.parse("link=https://example.com") shouldBe Style(link = "https://example.com")
        Style.parse("bold link=https://example.com") shouldBe Style(bold = true, link = "https://example.com")
    }

    "hex color" {
        Style.parse("#ff8800") shouldBe Style(color = Color.Rgb(0xff, 0x88, 0x00))
    }

    "ansi(N)" {
        Style.parse("ansi(208)") shouldBe Style(color = Color.AnsiValue(208))
    }

    "double color throws" {
        shouldThrow<IllegalStateException> { Style.parse("red green") }
    }

    "unknown token throws" {
        shouldThrow<IllegalStateException> { Style.parse("greenish") }
    }
})
