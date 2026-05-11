package tools.konsole.rich

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.core.style.Color

class HighlightersTest : StringSpec({

    "NULL highlighter does nothing" {
        val t = Text("hello 42")
        Highlighter.NULL.highlight(t)
        t.spansSnapshot.size shouldBe 0
    }

    "RegexHighlighter applies named-group → theme entry" {
        val theme = Theme(mapOf("number" to Style(color = Color.Red)))
        val h = RegexHighlighter(
            patterns = listOf(Regex("""(?<number>\d+)""")),
            theme = theme,
        )
        val t = Text("there are 42 lights")
        h.highlight(t)
        t.spansSnapshot.size shouldBe 1
        val span = t.spansSnapshot[0]
        // span covers "42"
        t.plain.substring(span.start, span.end) shouldBe "42"
        span.style.color shouldBe Color.Red
    }

    "DefaultHighlighter recognises numbers" {
        val theme = Theme(mapOf("repr.number" to Style(color = Color.Red)))
        val h = DefaultHighlighter(theme = theme)
        val t = Text("score is 99 today")
        h.highlight(t)
        t.spansSnapshot.size shouldBe 1
        t.plain.substring(t.spansSnapshot[0].start, t.spansSnapshot[0].end) shouldBe "99"
    }

    "DefaultHighlighter recognises URLs" {
        val theme = Theme(mapOf("repr.url" to Style(color = Color.Blue)))
        val h = DefaultHighlighter(theme = theme)
        val t = Text("see https://example.com for info")
        h.highlight(t)
        // Should have at least one span covering the URL
        val urlSpans = t.spansSnapshot.filter { it.style.color == Color.Blue }
        urlSpans.size shouldBe 1
        t.plain.substring(urlSpans[0].start, urlSpans[0].end) shouldBe "https://example.com"
    }
})
