package tools.konsole.rich

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.core.style.Color

private fun render(r: Renderable, width: Int = 20): String {
    val opts = RenderOptions(maxWidth = width)
    val sb = StringBuilder()
    val console = Console(terminal = null, writer = java.io.StringWriter(), width = width, colorSystem = tools.konsole.core.ColorSystem.None)
    for (s in r.render(console, opts)) sb.append(s.text)
    return sb.toString()
}

class WrappersTest : StringSpec({

    "Lines yields one line per Text with newline separators" {
        val l = Lines(Text("a"), Text("b"), Text("c"))
        render(l) shouldBe "a\nb\nc"
    }

    "Lines empty produces empty output" {
        render(Lines()) shouldBe ""
    }

    "Constrain caps maxWidth passed to inner renderable" {
        // Capture the maxWidth the inner renderable observes
        var observedWidth = -1
        val probe = Renderable { _, opts ->
            observedWidth = opts.maxWidth
            sequenceOf(Segment("x"))
        }
        val c = Constrain(probe, width = 10)
        render(c, width = 100)
        observedWidth shouldBe 10
    }

    "Constrain.measure caps the maximum" {
        val inner = Text("x".repeat(50))
        val c = Constrain(inner, width = 10)
        val opts = RenderOptions(maxWidth = 100)
        val console = Console(terminal = null, writer = java.io.StringWriter(), width = 100, colorSystem = tools.konsole.core.ColorSystem.None)
        c.measure(console, opts).maximum shouldBe 10
    }

    "Constrain with null width is transparent" {
        val t = Text("hello")
        val c = Constrain(t, width = null)
        render(c, width = 20) shouldBe render(t, width = 20)
    }

    "Styled overlays a base style on inner segments" {
        val inner = Text("hi")
        val styled = Styled(inner, Style(color = Color.Red))
        val opts = RenderOptions(maxWidth = 20)
        val console = Console(terminal = null, writer = java.io.StringWriter(), width = 20, colorSystem = tools.konsole.core.ColorSystem.None)
        val segs = styled.render(console, opts).toList()
        segs.first { it.text == "hi" }.style?.color shouldBe Color.Red
    }
})
