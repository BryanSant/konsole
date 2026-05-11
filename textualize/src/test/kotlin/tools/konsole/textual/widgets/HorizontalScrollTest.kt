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

class HorizontalScrollTest : StringSpec({

    "compositor drops leading scrollX cells from each rendered line" {
        val log = Log()
        log.write("abcdefghij")  // 10 chars
        log.scrollX = 4
        val compositor = Compositor(Region(0, 0, 6, 1))
        compositor.placeAt(log, Region(0, 0, 6, 1))
        val strips = compositor.render()
        val rendered = strips.first().segments.joinToString("") { it.text }
        // After scrolling right by 4, we should see "efghij" (6 chars) — possibly padded.
        rendered shouldContain "efghij"
        rendered shouldNotContain "abcd"
    }

    "Log scroll_right shifts scrollX by +2 per keypress" {
        val log = Log()
        log.write("0123456789")
        log.scrollX = 0
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(log)
        }
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Right))
            delay(40)
            (app.driver as HeadlessDriver).send(Key(KeyCode.Right))
            delay(40)
        }
        log.scrollX shouldBe 4
        app.stop()
    }

    "RichLog scroll_left does not go below 0" {
        val log = RichLog()
        log.write("hi")
        log.scrollX = 0
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(log)
        }
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Left))
            delay(40)
        }
        log.scrollX shouldBe 0
        app.stop()
    }
})
