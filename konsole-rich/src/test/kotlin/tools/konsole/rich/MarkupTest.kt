package tools.konsole.rich

import tools.konsole.rich.markup.Markup
import tools.konsole.core.style.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MarkupTest : StringSpec({

    "plain text" {
        Markup.parse("hello").plain shouldBe "hello"
    }

    "single tag" {
        val t = Markup.parse("[bold]hi[/]")
        t.plain shouldBe "hi"
    }

    "nested tags" {
        val t = Markup.parse("[bold]b [italic]bi[/italic] b[/]")
        t.plain shouldBe "b bi b"
    }

    "theme tag lookup" {
        val theme = Theme(mapOf("hi" to Style(color = Color.Cyan)))
        val t = Markup.parse("[hi]ok[/]", theme)
        t.plain shouldBe "ok"
    }

    "compound style" {
        val t = Markup.parse("[bold red on white]hi[/]")
        t.plain shouldBe "hi"
    }

    "link= attribute" {
        val t = Markup.parse("[link=https://example.com]click[/]")
        t.plain shouldBe "click"
    }

    "escape with backslash" {
        val t = Markup.parse("\\[not a tag]")
        t.plain shouldBe "[not a tag]"
    }

    "unmatched close ignored" {
        Markup.parse("[/]").plain shouldBe ""
        Markup.parse("text[/]more").plain shouldBe "textmore"
    }

    "unmatched open auto-closed" {
        // not error; just renders the text
        Markup.parse("[bold]oops").plain shouldBe "oops"
    }

    "named tag close" {
        val t = Markup.parse("[bold]a [italic]b[/italic] c[/bold]")
        t.plain shouldBe "a b c"
    }

    "emoji substitution" {
        val t = Markup.parse(":rocket: launching")
        t.plain shouldBe "🚀 launching"
    }

    "unknown emoji left literal" {
        val t = Markup.parse(":notarealemojiname:")
        t.plain shouldBe ":notarealemojiname:"
    }

    "escape util roundtrips" {
        val raw = "this [is brackets]"
        val escaped = Markup.escape(raw)
        Markup.parse(escaped).plain shouldBe raw
    }
})
