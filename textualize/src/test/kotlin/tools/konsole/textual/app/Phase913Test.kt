package tools.konsole.textual.app

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Click
import tools.konsole.textual.events.MouseButton
import tools.konsole.textual.events.MouseDown
import tools.konsole.textual.events.MouseDrag
import tools.konsole.textual.events.MouseUp
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.Label

private class AutoFocusApp : App(HeadlessDriver()) {
    val btn1 = Button("a", id = "a")
    val btn2 = Button("b", id = "b")
    override fun compose() = sequenceOf<Widget>(btn1, btn2)
}

private class DisableAutoFocusApp : App(HeadlessDriver()) {
    val btn1 = Button("a", id = "a")
    override val autoFocusOnStart: Boolean get() = false
    override fun compose() = sequenceOf<Widget>(btn1)
}

private class MouseApp : App(HeadlessDriver()) {
    val btn = Button("click me", id = "btn")
    override fun compose() = sequenceOf<Widget>(btn)
}

class Phase913Test : StringSpec({

    "App.start auto-focuses the first focusable widget" {
        val app = AutoFocusApp()
        app.start()
        app.focused shouldBe app.btn1
        app.stop()
    }

    "autoFocusOnStart=false skips the auto-focus" {
        val app = DisableAutoFocusApp()
        app.start()
        app.focused shouldBe null
        app.stop()
    }

    "MouseDown sets pressedWidget and marks isPressed=true" {
        val app = MouseApp()
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()
        runBlocking {
            (app.driver as HeadlessDriver).send(MouseDown(0, 0, MouseButton.Left))
            delay(50)
        }
        app.btn.isPressed shouldBe true
        app.stop()
    }

    "MouseUp on the same widget synthesises a Click" {
        val app = MouseApp()
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()
        app.btn.start()
        var clickReceived = false
        app.btn.onMessage<Click> { clickReceived = true }
        runBlocking {
            (app.driver as HeadlessDriver).send(MouseDown(0, 0, MouseButton.Left))
            delay(30)
            (app.driver as HeadlessDriver).send(MouseUp(0, 0, MouseButton.Left))
            delay(80)
        }
        clickReceived shouldBe true
        app.btn.isPressed shouldBe false
        app.stop()
    }

    "MouseUp on a different widget does NOT synthesise a Click" {
        val a = Button("a", id = "a")
        val b = Button("b", id = "b")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(a, b)
        }
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()
        a.start(); b.start()
        var clickA = false; var clickB = false
        a.onMessage<Click> { clickA = true }
        b.onMessage<Click> { clickB = true }
        runBlocking {
            (app.driver as HeadlessDriver).send(MouseDown(0, 0, MouseButton.Left))   // press a
            delay(20)
            (app.driver as HeadlessDriver).send(MouseUp(0, 1, MouseButton.Left))      // release on b
            delay(80)
        }
        clickA shouldBe false
        clickB shouldBe false
        app.stop()
    }

    "MouseMove while pressed fires MouseDrag at the held widget" {
        val app = MouseApp()
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()
        app.btn.start()
        var lastDrag: MouseDrag? = null
        app.btn.onMessage<MouseDrag> { lastDrag = it }
        runBlocking {
            (app.driver as HeadlessDriver).send(MouseDown(2, 0, MouseButton.Left))
            delay(20)
            (app.driver as HeadlessDriver).send(tools.konsole.textual.events.MouseMove(10, 0))
            delay(80)
        }
        lastDrag?.startX shouldBe 2
        lastDrag?.x shouldBe 10
        app.stop()
    }
})
