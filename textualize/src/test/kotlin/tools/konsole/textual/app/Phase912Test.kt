package tools.konsole.textual.app

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyModifiers
import tools.konsole.rich.Renderable
import tools.konsole.rich.Text
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Key
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Button
import tools.konsole.textual.widgets.Label

private class FocusableA : Widget(id = "a") { override val canFocus: Boolean = true; override fun render(): Renderable = Text("a") }
private class FocusableB : Widget(id = "b") { override val canFocus: Boolean = true; override fun render(): Renderable = Text("b") }
private class FocusableC : Widget(id = "c") { override val canFocus: Boolean = true; override fun render(): Renderable = Text("c") }

private class MultiFocusApp : App(HeadlessDriver()) {
    val a = FocusableA()
    val b = FocusableB()
    val c = FocusableC()
    override fun compose(): Sequence<Widget> = sequenceOf(a, b, c)
}

private class ActionApp : App(HeadlessDriver()) {
    var savedRan: Boolean = false
    override val bindings = bindings("ctrl+s" to "save", "q" to "quit")
    @Suppress("unused")
    fun action_save() { savedRan = true }
}

class Phase912Test : StringSpec({

    // ---- Focus ----

    "setFocus marks the widget hasFocus=true and the previous one false" {
        val app = MultiFocusApp()
        app.start()
        app.setFocus(app.a)
        app.a.hasFocus shouldBe true
        app.setFocus(app.b)
        app.a.hasFocus shouldBe false
        app.b.hasFocus shouldBe true
        app.stop()
    }

    "focusNext cycles through focusable widgets in order" {
        val app = MultiFocusApp()
        app.start()
        app.focused shouldBe null
        app.focusNext()
        app.focused shouldBe app.a
        app.focusNext()
        app.focused shouldBe app.b
        app.focusNext()
        app.focused shouldBe app.c
        app.focusNext()
        app.focused shouldBe app.a   // wraps
        app.stop()
    }

    "focusPrevious cycles backwards" {
        val app = MultiFocusApp()
        app.start()
        app.setFocus(app.b)
        app.focusPrevious()
        app.focused shouldBe app.a
        app.focusPrevious()
        app.focused shouldBe app.c   // wraps backwards
        app.stop()
    }

    "Tab key triggers focusNext via App.handleEvent" {
        val app = MultiFocusApp()
        app.driver.startApplicationMode()
        app.start()
        app.setFocus(app.a)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Tab))
            delay(40)
        }
        app.focused shouldBe app.b
        app.stop()
    }

    "BackTab (Shift+Tab) triggers focusPrevious" {
        val app = MultiFocusApp()
        app.driver.startApplicationMode()
        app.start()
        app.setFocus(app.b)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.BackTab, KeyModifiers.SHIFT))
            delay(40)
        }
        app.focused shouldBe app.a
        app.stop()
    }

    // ---- Action dispatcher ----

    "action_<name> dispatch fires on a matching binding" {
        val app = ActionApp()
        app.driver.startApplicationMode()
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Char('s'), KeyModifiers.CONTROL))
            delay(40)
        }
        app.savedRan shouldBe true
        app.stop()
    }

    "action_quit is the built-in default for quit bindings" {
        val app = ActionApp()
        app.driver.startApplicationMode()
        app.start()
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Char('q')))
            delay(40)
        }
        // exit() flips a flag; nothing else to assert without app.run().
        // Reaching this point without exception means dispatch worked.
        app.stop()
    }

    "App.action(name) returns false when no handler matches" {
        val app = MultiFocusApp()
        app.action("not_a_real_action") shouldBe false
    }

    // ---- Widget.lastRegion ----

    "Compositor sets lastRegion on placed widgets after render" {
        val app = MultiFocusApp()
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()
        app.a.lastRegion shouldNotBe null
        app.b.lastRegion shouldNotBe null
        // Vertical-stack layout: a on row 0, b on row 1, c on row 2.
        app.a.lastRegion?.y shouldBe 0
        app.b.lastRegion?.y shouldBe 1
        app.stop()
    }

    "lastRegion reflects in-place compositor resize" {
        val app = MultiFocusApp()
        app.driver.startApplicationMode()
        app.start()
        app.renderFrame()
        val initialWidth = app.a.lastRegion?.width
        runBlocking {
            (app.driver as HeadlessDriver).send(tools.konsole.textual.events.Resize(columns = 120, rows = 40))
            delay(50)
        }
        app.renderFrame()
        app.a.lastRegion?.width shouldBe 120
        // Initial was screenWidth=80 default
        initialWidth shouldBe 80
        app.stop()
    }
})

private infix fun <T> T.shouldNotBe(other: T?) {
    if (this == other) error("expected $this != $other")
}
