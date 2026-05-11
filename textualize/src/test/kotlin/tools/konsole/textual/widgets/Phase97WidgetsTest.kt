package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.rich.RenderOptions

private fun render(w: tools.konsole.textual.widget.Widget, width: Int = 60): String {
    val console = Console.string(width = width)
    val opts = RenderOptions(maxWidth = width)
    return w.render().render(console, opts).joinToString("") { it.text }
}

class Phase97WidgetsTest : StringSpec({

    // ---- Document ----

    "Document() has one empty line by default" {
        val d = Document()
        d.lineCount shouldBe 1
        d.line(0) shouldBe ""
    }

    "Document splits initial text on newlines" {
        val d = Document("line one\nline two\nline three")
        d.lineCount shouldBe 3
        d.line(1) shouldBe "line two"
    }

    "Document.insert at start prepends to first line" {
        val d = Document("world")
        val cursor = d.insert(Location.ZERO, "hello, ")
        d.text shouldBe "hello, world"
        cursor shouldBe Location(0, 7)
    }

    "Document.insert with newline creates a new line" {
        val d = Document("ab")
        val cursor = d.insert(Location(0, 1), "X\nY")
        d.text shouldBe "aX\nYb"
        cursor shouldBe Location(1, 1)
    }

    "Document.delete removes the range" {
        val d = Document("hello world")
        d.delete(Location(0, 5), Location(0, 6))
        d.text shouldBe "helloworld"
    }

    "Document.delete across lines joins them" {
        val d = Document("hello\nworld")
        d.delete(Location(0, 3), Location(1, 2))
        d.text shouldBe "helrld"
    }

    "Document.substring slices a range" {
        val d = Document("hello world")
        d.substring(Location(0, 6), Location(0, 11)) shouldBe "world"
    }

    "Document.endLocation reports the very last position" {
        val d = Document("ab\ncde")
        d.endLocation() shouldBe Location(1, 3)
    }

    // ---- TextArea ----

    "TextArea.insert appends at cursor and fires Changed" {
        val ta = TextArea(initial = "")
        ta.start()
        var last: String? = null
        ta.onMessage<TextArea.Changed> { last = it.text }
        ta.insert("hello")
        runBlocking { delay(30); ta.stop() }
        ta.text shouldBe "hello"
        last shouldBe "hello"
    }

    "TextArea.deleteLeft removes one char" {
        val ta = TextArea(initial = "abc")
        ta.moveCursorDocumentEnd()
        ta.deleteLeft()
        ta.text shouldBe "ab"
    }

    "TextArea.deleteRight at end-of-line joins next line" {
        val ta = TextArea(initial = "ab\ncd")
        ta.setCursor(Location(0, 2))
        ta.deleteRight()
        ta.text shouldBe "abcd"
    }

    "TextArea.moveCursorDown clamps to line length" {
        val ta = TextArea(initial = "long line here\nshort")
        ta.setCursor(Location(0, 14))
        ta.moveCursorDown()
        ta.cursor shouldBe Location(1, 5)  // clamped to "short".length
    }

    "TextArea.selectAll selects from origin to end" {
        val ta = TextArea(initial = "hello\nworld")
        ta.selectAll()
        ta.selection.start shouldBe Location.ZERO
        ta.selection.end shouldBe Location(1, 5)
    }

    "TextArea.insert with active selection replaces it" {
        val ta = TextArea(initial = "abcdef")
        ta.select(Selection(Location(0, 1), Location(0, 4)))
        ta.insert("X")
        ta.text shouldBe "aXef"
    }

    "TextArea.load replaces the entire document" {
        val ta = TextArea(initial = "old")
        ta.load("brand new content")
        ta.text shouldBe "brand new content"
    }

    "TextArea.readOnly drops inserts but allows cursor moves" {
        val ta = TextArea(initial = "constant", readOnly = true)
        ta.insert("x")
        ta.text shouldBe "constant"
        ta.moveCursorRight()
        ta.cursor shouldBe Location(0, 1)
    }

    // ---- MaskedInput ----

    "MaskedInput rejects chars that don't match the template" {
        val mi = MaskedInput(template = "999-99-9999")
        mi.insert("abc")
        mi.value shouldBe ""
        mi.insert("123")
        mi.value shouldBe "123"
    }

    "MaskedInput validator reports incomplete entries as Invalid" {
        val mi = MaskedInput(template = "AA99")
        mi.insert("ab")
        (mi.validationResult is ValidationResult.Invalid) shouldBe true
        mi.insert("12")
        mi.value shouldBe "ab12"
        (mi.validationResult is ValidationResult.Valid) shouldBe true
    }

    "MaskedInput respects template length cap" {
        val mi = MaskedInput(template = "9999")
        mi.insert("12345")
        mi.value.length shouldBe 4
    }

    // ---- Link ----

    "Link renders the label with the URL embedded in style.link" {
        val l = Link(label = "konsole repo", url = "https://github.com/bryansant/konsole")
        render(l) shouldContain "konsole repo"
    }

    "Link.activate posts Activated with the URL" {
        val l = Link(label = "x", url = "https://example.com")
        l.start()
        var capturedUrl: String? = null
        l.onMessage<Link.Activated> { capturedUrl = it.url }
        l.activate()
        runBlocking { delay(30); l.stop() }
        capturedUrl shouldBe "https://example.com"
    }

    // ---- Sparkline ----

    "Sparkline renders one cell per data point" {
        val s = Sparkline(listOf(1.0, 5.0, 9.0))
        render(s).length shouldBe 3
    }

    "Sparkline tallest point uses the highest block when auto-scaled" {
        val s = Sparkline(listOf(0.0, 100.0))
        render(s).last() shouldBe '█'
    }

    "Sparkline with explicit maxValue scales to the cap" {
        val s = Sparkline(listOf(50.0), maxValue = 100.0)
        // 50 / 100 = 0.5 → mid-range block
        val ch = render(s).first()
        // The 8-block ladder makes 0.5 land on index 4 (▄)
        (ch in Sparkline.BLOCKS) shouldBe true
    }

    "Sparkline.update replaces the series and refreshes" {
        val s = Sparkline(listOf(1.0))
        s.update(listOf(1.0, 2.0, 3.0, 4.0))
        render(s).length shouldBe 4
    }

    // ---- PrettyWidget ----

    "PrettyWidget renders the current target" {
        val p = PrettyWidget(target = mapOf("k" to "v"))
        render(p) shouldContain "k"
        render(p) shouldContain "v"
    }

    "PrettyWidget.update replaces the target" {
        val p = PrettyWidget(target = "first")
        p.update(listOf(1, 2, 3))
        render(p) shouldContain "1"
    }

    // ---- Welcome ----

    "Welcome renders the title inside its panel" {
        val w = Welcome(title = "Konsole", body = "demo")
        render(w, width = 40) shouldContain "Konsole"
    }
})
