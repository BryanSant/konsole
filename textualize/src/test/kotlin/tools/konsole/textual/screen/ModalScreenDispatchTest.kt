package tools.konsole.textual.screen

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.core.event.KeyCode
import tools.konsole.textual.app.App
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Key
import tools.konsole.textual.widget.Widget

class ModalScreenDispatchTest : StringSpec({

    "Esc on a pushed InputScreen runs action_dismiss inherited from ModalScreen" {
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>()
        }
        app.start()
        val dialog = InputScreen(prompt = "Name?", placeholder = "type…")
        var result: String? = "untouched"
        dialog.onResult { result = it }
        app.pushScreen(dialog)

        // After push, dialog is currentScreen and its Input is focused.
        app.currentScreen shouldBe dialog
        app.focused shouldBe dialog.input

        // Inject an Esc keypress through the headless driver.
        val driver = app.driver as HeadlessDriver
        runBlocking {
            driver.send(Key(KeyCode.Esc))
            delay(60)
        }
        // onResult should have fired with null and the dialog popped off.
        result shouldBe null
        app.currentScreen shouldNotBe dialog
        app.stop()
    }

    "Shift+B in a focused Input inserts B" {
        val app = object : App(HeadlessDriver()) {
            override fun compose() = sequenceOf<Widget>()
        }
        app.start()
        val dialog = InputScreen(prompt = "Name?", placeholder = "type…")
        app.pushScreen(dialog)

        val driver = app.driver as HeadlessDriver
        runBlocking {
            // Shifted 'B' as the parser would produce after REPORT_ALTERNATE_KEYS stripping.
            driver.send(Key(KeyCode.Char('B'), tools.konsole.core.event.KeyModifiers.NONE))
            delay(40)
            // And once with the Shift modifier still present, simulating a terminal
            // that didn't send the alt-codepoint.
            driver.send(Key(KeyCode.Char('B'), tools.konsole.core.event.KeyModifiers.SHIFT))
            delay(40)
        }
        dialog.input.value shouldBe "BB"
        app.stop()
    }
})
