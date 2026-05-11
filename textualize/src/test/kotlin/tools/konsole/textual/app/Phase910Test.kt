package tools.konsole.textual.app

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.compositor.Compositor
import tools.konsole.textual.compositor.StripSerializer
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.MouseMove
import tools.konsole.textual.events.Resize
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.Label

private class TickApp : App(HeadlessDriver()) {
    val label: Label = Label("static")
    override fun compose() = sequenceOf<Widget>(label)
}

class Phase910Test : StringSpec({

    "dirty-region diff emits zero bytes when nothing changes" {
        val app = TickApp()
        app.driver.startApplicationMode()
        app.renderFrame()
        (app.driver as HeadlessDriver).clearOutput()
        // Second frame: nothing changed. The diff should be empty.
        app.renderFrame()
        app.driver.output shouldBe ""
    }

    "StripSerializer.serializeDiff skips matching rows and only emits changed ones" {
        val makeStrip = { text: String -> tools.konsole.rich.Strip.of(tools.konsole.rich.Segment(text)) }
        val old = listOf(makeStrip("aaa"), makeStrip("bbb"), makeStrip("ccc"))
        val new = listOf(makeStrip("aaa"), makeStrip("BBB"), makeStrip("ccc"))
        val out = StripSerializer.serializeDiff(old, new)
        out shouldContain "BBB"
        out shouldNotContain "aaa"
        out shouldNotContain "ccc"
    }

    "invalidate() forces a full repaint on the next frame" {
        val app = TickApp()
        app.driver.startApplicationMode()
        app.renderFrame()
        (app.driver as HeadlessDriver).clearOutput()
        app.invalidate()
        app.renderFrame()
        // Output non-empty (full repaint after invalidate)
        app.driver.output.isNotEmpty() shouldBe true
    }

    "Compositor.resize updates the viewport in place" {
        val c = Compositor(Region(0, 0, 80, 24))
        c.viewport.width shouldBe 80
        c.resize(Region(0, 0, 120, 40))
        c.viewport.width shouldBe 120
        c.viewport.height shouldBe 40
    }

    "Resize event updates Compositor viewport and invalidates frame" {
        val app = TickApp()
        app.driver.startApplicationMode()
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Resize(columns = 120, rows = 40))
            delay(50)
        }
        app.screenWidth shouldBe 120
        app.compositor.viewport.width shouldBe 120
        app.compositor.viewport.height shouldBe 40
        app.stop()
    }

    "Widget.isHovered flips on MouseMove" {
        val btn = Button("hover me")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(btn)
        }
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()                          // arrange — Button placed at row 0
        runBlocking {
            (app.driver as HeadlessDriver).send(MouseMove(x = 1, y = 0))
            delay(50)
        }
        btn.isHovered shouldBe true
        app.stop()
    }

    "Widget.isHovered clears when pointer moves off" {
        val btn = Button("a")
        val btn2 = Button("b")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(btn, btn2)
        }
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()
        runBlocking {
            (app.driver as HeadlessDriver).send(MouseMove(x = 0, y = 0))
            delay(40)
            btn.isHovered shouldBe true
            app.driver.send(MouseMove(x = 0, y = 1))
            delay(40)
        }
        btn.isHovered shouldBe false
        btn2.isHovered shouldBe true
        app.stop()
    }
})
