package tools.konsole.textual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.textual.message.Message
import tools.konsole.textual.message.MessagePump

data class Submit(val payload: String) : Message()

class MessagePumpTest : StringSpec({

    "post + handler dispatches to registered listener" {
        val pump = MessagePump()
        var seen: String? = null
        pump.onMessage<Submit> { seen = it.payload }
        pump.start()
        pump.post(Submit("hello"))
        runBlocking { delay(50); pump.stop() }
        seen shouldBe "hello"
    }

    "handlers fire in registration order" {
        val pump = MessagePump()
        val seen = mutableListOf<Int>()
        pump.onMessage<Submit> { seen += 1 }
        pump.onMessage<Submit> { seen += 2 }
        pump.start()
        pump.post(Submit("x"))
        runBlocking { delay(50); pump.stop() }
        seen shouldBe listOf(1, 2)
    }

    "stop() releases the consumer coroutine" {
        val pump = MessagePump()
        pump.start()
        pump.isRunning shouldBe true
        pump.stop()
        runBlocking { delay(50) }
        pump.isRunning shouldBe false
    }

    "isStopped on a message halts further handlers" {
        val pump = MessagePump()
        var second = false
        pump.onMessage<Submit> { it.stop() }
        pump.onMessage<Submit> { second = true }
        pump.start()
        pump.post(Submit("x"))
        runBlocking { delay(50); pump.stop() }
        second shouldBe false
    }
})
