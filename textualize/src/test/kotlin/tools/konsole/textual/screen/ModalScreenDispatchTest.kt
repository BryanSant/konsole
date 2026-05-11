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

    "Esc dismisses on the first press even when the App has its own bindings" {
        // Reproduces the DialogDemo shape: the App declares per-letter
        // bindings (which match before the modal is pushed) and a modal
        // is opened via one of those bindings. Esc should dismiss on the
        // very first press, not require a second.
        val app = object : App(HeadlessDriver()) {
            override val bindings = tools.konsole.textual.binding.bindings(
                "ctrl+c" to "quit",
                "b" to "toggle_banner",
                "i" to "open_input",
            )
            @Suppress("unused") public fun action_toggle_banner() {}
            @Suppress("unused") public fun action_open_input() {
                val d = InputScreen(prompt = "Name?")
                pushScreen(d)
            }
            override fun compose() = sequenceOf<Widget>()
        }
        app.start()
        val driver = app.driver as HeadlessDriver
        // Press 'i' to open the input dialog.
        runBlocking {
            driver.send(Key(KeyCode.Char('i')))
            delay(40)
        }
        val dialog = app.currentScreen as? InputScreen
            ?: error("InputScreen not pushed after 'i' press")
        var result: String? = "untouched"
        dialog.onResult { result = it }

        // Single Esc press should dismiss.
        runBlocking {
            driver.send(Key(KeyCode.Esc))
            delay(60)
        }
        result shouldBe null
        app.currentScreen shouldNotBe dialog
        app.stop()
    }

    "After dismissing a modal, the same binding can reopen it" {
        // Bug repro: opening InputScreen via 'i', then dismissing it (Esc or
        // Enter), used to leave `App.focused` pointing at the popped Input.
        // The next 'i' press would be consumed by that detached widget as
        // text input and never reach the App-level "i" → "open_input"
        // binding, so the dialog could only be opened once.
        val app = object : App(HeadlessDriver()) {
            override val bindings = tools.konsole.textual.binding.bindings(
                "i" to "open_input",
            )
            @Suppress("unused") public fun action_open_input() {
                pushScreen(InputScreen(prompt = "Name?"))
            }
            override fun compose() = sequenceOf<Widget>()
        }
        app.start()
        val driver = app.driver as HeadlessDriver

        runBlocking { driver.send(Key(KeyCode.Char('i'))); delay(40) }
        val first = app.currentScreen as? InputScreen
            ?: error("InputScreen not pushed after first 'i'")

        runBlocking { driver.send(Key(KeyCode.Esc)); delay(60) }
        app.currentScreen shouldNotBe first

        // The second 'i' must reach the App binding and push a fresh modal.
        runBlocking { driver.send(Key(KeyCode.Char('i'))); delay(40) }
        val second = app.currentScreen as? InputScreen
            ?: error("InputScreen not re-pushed after second 'i'")
        (second === first) shouldBe false
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
