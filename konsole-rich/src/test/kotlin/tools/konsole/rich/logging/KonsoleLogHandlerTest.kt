package tools.konsole.rich.logging

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.StringWriter
import java.util.logging.Level
import java.util.logging.LogRecord

class KonsoleLogHandlerTest : StringSpec({

    "INFO record formats with level and message" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val handler = KonsoleLogHandler(c, showTime = false, showPath = false)
        val record = LogRecord(Level.INFO, "hello world")
        handler.publish(record)
        val out = sw.toString()
        out shouldContain "INFO"
        out shouldContain "hello world"
    }

    "ERROR record displays elevated level" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val handler = KonsoleLogHandler(c, showTime = false, showPath = false)
        val record = LogRecord(Level.SEVERE, "bad thing")
        handler.publish(record)
        val out = sw.toString()
        out shouldContain "ERROR"
        out shouldContain "bad thing"
    }

    "Throwable in record is rendered via Traceback" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val handler = KonsoleLogHandler(c, showTime = false, showPath = false)
        val record = LogRecord(Level.SEVERE, "operation failed").apply {
            thrown = IllegalStateException("inner")
        }
        handler.publish(record)
        val out = sw.toString()
        out shouldContain "operation failed"
        out shouldContain "IllegalStateException"
    }

    "showPath=false omits source class" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val handler = KonsoleLogHandler(c, showTime = false, showPath = false)
        val record = LogRecord(Level.INFO, "x")
        record.sourceClassName = "com.example.NotShown"
        handler.publish(record)
        sw.toString() shouldNotContain "com.example.NotShown"
    }
})
