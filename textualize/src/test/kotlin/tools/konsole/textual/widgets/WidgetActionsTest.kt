package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyModifiers
import tools.konsole.textual.app.App
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Key
import tools.konsole.textual.widget.Widget

class WidgetActionsTest : StringSpec({

    "Button.action_press triggers a Pressed message" {
        val btn = Button("ok")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(btn)
        }
        var pressed = false
        app.start()
        btn.start()
        btn.onMessage<Button.Pressed> { pressed = true }
        app.setFocus(btn)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Enter))
            delay(50)
        }
        pressed shouldBe true
        app.stop()
    }

    "Switch.action_toggle flips value via Enter binding" {
        val sw = Switch(initial = false)
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(sw)
        }
        app.start()
        sw.start()
        app.setFocus(sw)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Char(' ')))
            delay(50)
        }
        sw.value shouldBe true
        app.stop()
    }

    "Checkbox.action_toggle flips value via Space binding" {
        val cb = Checkbox(label = "ok")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(cb)
        }
        app.start()
        cb.start()
        app.setFocus(cb)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Char(' ')))
            delay(50)
        }
        cb.value shouldBe true
        app.stop()
    }

    "OptionList.action_cursor_down moves the highlight via arrow key" {
        val ol = OptionList.ofLabels(listOf("Alpha", "Beta", "Gamma"))
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(ol)
        }
        app.start()
        ol.start()
        app.setFocus(ol)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Down))
            delay(50)
        }
        ol.highlightedIndex shouldBe 1
        app.stop()
    }

    "Tree action_expand opens a collapsed node" {
        val tree = Tree<String>("root")
        tree.root.expanded = false
        tree.root.addLeaf("a")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(tree)
        }
        app.start()
        tree.start()
        app.setFocus(tree)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Right))
            delay(50)
        }
        tree.root.expanded shouldBe true
        app.stop()
    }

    "Input inserts printable chars on Key events" {
        val input = Input(initial = "")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(input)
        }
        app.start()
        input.start()
        app.setFocus(input)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Char('h')))
            app.driver.send(Key(KeyCode.Char('i')))
            delay(100)
        }
        input.value shouldBe "hi"
        app.stop()
    }

    "Input.action_delete_left removes a char via Backspace binding" {
        val input = Input(initial = "abc")
        input.moveCursorTo(3)
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(input)
        }
        app.start()
        input.start()
        app.setFocus(input)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Backspace))
            delay(50)
        }
        input.value shouldBe "ab"
        app.stop()
    }

    "TextArea inserts chars and Enter creates a newline via action_newline" {
        val ta = TextArea(initial = "")
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(ta)
        }
        app.start()
        ta.start()
        app.setFocus(ta)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Char('a')))
            app.driver.send(Key(KeyCode.Enter))
            app.driver.send(Key(KeyCode.Char('b')))
            delay(100)
        }
        ta.text shouldBe "a\nb"
        app.stop()
    }

    "TextArea.readOnly drops printable keys" {
        val ta = TextArea(initial = "fixed", readOnly = true)
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>(ta)
        }
        app.start()
        ta.start()
        app.setFocus(ta)
        runBlocking {
            (app.driver as HeadlessDriver).send(Key(KeyCode.Char('x')))
            delay(50)
        }
        ta.text shouldBe "fixed"
        app.stop()
    }
})
