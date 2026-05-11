package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.rich.RenderOptions
import tools.konsole.textual.widget.Scrollable

private fun render(w: tools.konsole.textual.widget.Widget, width: Int = 30): String {
    val console = Console.string(width = width)
    val opts = RenderOptions(maxWidth = width)
    return w.render().render(console, opts).joinToString(separator = "") { it.text }
}

private class FakeScrollable(
    override val contentWidth: Int = 100,
    override val contentHeight: Int = 50,
) : Scrollable {
    override var scrollX: Int = 0
    override var scrollY: Int = 0
}

class WidgetsTest : StringSpec({

    "Static renders its initial markup content" {
        val s = Static("[bold]hi[/]")
        render(s) shouldContain "hi"
    }

    "Static.update replaces content" {
        val s = Static("first")
        s.update("second")
        render(s) shouldContain "second"
    }

    "Label is a Static subclass" {
        val l = Label("hello")
        render(l) shouldContain "hello"
    }

    "Rule renders the divider character" {
        val r = Rule()
        render(r, width = 10) shouldContain "─"
    }

    "Rule honors custom char" {
        val r = Rule(char = '=')
        render(r, width = 10) shouldContain "="
    }

    "LoadingIndicator renders one of the dots-spinner frames" {
        val li = LoadingIndicator()
        val out = render(li, width = 10)
        // dots spinner frames are 1-cell unicode dots — any frame is non-empty
        out.length shouldBe 1
    }

    "ProgressBar widget renders full when complete" {
        val pb = ProgressBar(total = 100.0, progress = 100.0)
        val out = render(pb, width = 10)
        out.length shouldBe 10
    }

    "Button posts Pressed when press() is called" {
        val b = Button("Click me", variant = ButtonVariant.Primary)
        b.start()
        var pressed = false
        b.onMessage<Button.Pressed> { pressed = true }
        b.press()
        runBlocking { delay(50); b.stop() }
        pressed shouldBe true
    }

    "Switch.toggle flips value and posts Changed" {
        val s = Switch(initial = false)
        s.start()
        var lastValue: Boolean? = null
        s.onMessage<Switch.Changed> { lastValue = it.value }
        s.toggle()
        runBlocking { delay(50); s.stop() }
        s.value shouldBe true
        lastValue shouldBe true
    }

    "Checkbox.toggle flips value and posts Changed" {
        val c = Checkbox("Agree", initial = false)
        c.start()
        var lastValue: Boolean? = null
        c.onMessage<Checkbox.Changed> { lastValue = it.value }
        c.toggle()
        runBlocking { delay(50); c.stop() }
        c.value shouldBe true
        lastValue shouldBe true
    }

    "Header renders the title with the icon" {
        val h = Header(title = "MyApp")
        render(h, width = 30) shouldContain "MyApp"
    }

    "Footer renders all visible bindings" {
        val map = tools.konsole.textual.binding.bindings("q" to "quit", "ctrl+c" to "cancel")
        val f = Footer(map)
        val out = render(f, width = 40)
        out shouldContain "q"
        out shouldContain "ctrl+c"
    }

    "Scrollable mixin clamps scrollTo at content edges" {
        val s = FakeScrollable(contentWidth = 10, contentHeight = 5)
        s.scrollTo(x = 100, y = 100)
        s.scrollX shouldBe 9
        s.scrollY shouldBe 4
    }

    "Scrollable.scrollHome resets to origin" {
        val s = FakeScrollable()
        s.scrollTo(50, 25)
        s.scrollHome()
        s.scrollX shouldBe 0
        s.scrollY shouldBe 0
    }
})
