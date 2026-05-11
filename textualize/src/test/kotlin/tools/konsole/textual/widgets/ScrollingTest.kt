package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.core.event.KeyCode
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.app.App
import tools.konsole.textual.compositor.Compositor
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Key
import tools.konsole.textual.widget.Widget

class ScrollingTest : StringSpec({

    "RichLog only renders lines in the visible viewport via scrollY" {
        val log = RichLog()
        for (i in 0 until 20) log.write("line $i")
        // 5-row viewport starting at scrollY=10
        log.scrollY = 10
        val compositor = Compositor(Region(0, 0, 80, 5))
        compositor.placeAt(log, Region(0, 0, 80, 5))
        val strips = compositor.render()
        val flat = strips.joinToString("\n") { it.segments.joinToString("") { s -> s.text } }
        flat shouldContain "line 10"
        flat shouldContain "line 11"
        flat shouldContain "line 14"
        flat shouldNotContain "line 0 "  // off-screen above
        flat shouldNotContain "line 15"  // off-screen below
    }

    "Log scrolling via Down arrow key + binding" {
        val log = Log()
        for (i in 0 until 30) log.write("entry $i")
        log.scrollY = 0
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(log)
        }
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Down))
            app.driver.send(Key(KeyCode.Down))
            app.driver.send(Key(KeyCode.Down))
            delay(80)
        }
        log.scrollY shouldBe 3
        app.stop()
    }

    "MarkdownViewer scroll_end goes to the bottom" {
        val viewer = MarkdownViewer(markdown = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(viewer)
        }
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.End))
            delay(50)
        }
        viewer.scrollY shouldBe viewer.contentHeight - 1
        app.stop()
    }

    "scroll_home returns to top" {
        val log = RichLog()
        for (i in 0 until 10) log.write("entry $i")
        log.scrollY = 5
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(log)
        }
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Home))
            delay(50)
        }
        log.scrollY shouldBe 0
        app.stop()
    }

    "scrolling clamps at content edges" {
        val log = Log()
        for (i in 0 until 5) log.write("x$i")
        log.scrollY = 0
        log.scrollBy(dy = -10)
        log.scrollY shouldBe 0
        log.scrollBy(dy = 100)
        log.scrollY shouldBe 4  // contentHeight - 1
    }

    "compositor passes scrollY+localY to widget.renderLine for Scrollable widgets" {
        val log = RichLog()
        for (i in 0 until 100) log.write("L$i")
        log.scrollY = 50
        val compositor = Compositor(Region(0, 0, 80, 3))
        compositor.placeAt(log, Region(0, 0, 80, 3))
        val strips = compositor.render()
        val rendered = strips.joinToString("\n") { it.segments.joinToString("") { s -> s.text } }
        rendered shouldContain "L50"
        rendered shouldContain "L51"
        rendered shouldContain "L52"
        rendered shouldNotContain "L49"
        rendered shouldNotContain "L53"
    }
})
