package tools.konsole.textual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import tools.konsole.core.event.KeyCode
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.events.Key

class HeadlessDriverTest : StringSpec({

    "write accumulates into the output buffer" {
        val d = HeadlessDriver()
        d.write("hello, ")
        d.write("world\n")
        d.output shouldBe "hello, world\n"
    }

    "clearOutput resets the buffer" {
        val d = HeadlessDriver()
        d.write("x")
        d.clearOutput()
        d.output shouldBe ""
    }

    "send delivers events through the flow" {
        val d = HeadlessDriver()
        val k = Key(KeyCode.Char('q'))
        d.send(k)
        runBlocking {
            val first = d.events.first()
            (first as? Key)?.code shouldBe KeyCode.Char('q')
        }
    }

    "disabled input drops events" {
        val d = HeadlessDriver()
        d.disableInput()
        d.send(Key(KeyCode.Char('x')))
        // Re-enable so we don't block when reading the stream
        d.enableInput()
        d.send(Key(KeyCode.Char('y')))
        runBlocking {
            val ev = d.events.first()
            (ev as? Key)?.code shouldBe KeyCode.Char('y')
        }
    }
})
