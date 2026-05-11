package tools.konsole.core.integration

import tools.konsole.core.alternateScreen
import tools.konsole.core.execute
import tools.konsole.core.MoveTo
import tools.konsole.core.style.Print
import tools.konsole.core.synchronizedUpdate
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class DumbTerminalSpec : StringSpec({

    "Writer.alternateScreen wraps block in CSI ?1049h / l" {
        val w = StringWriter()
        w.alternateScreen {
            it.execute(MoveTo(0, 0), Print("hello"))
        }
        w.toString() shouldContain "[?1049h"
        w.toString() shouldContain "[1;1H"
        w.toString() shouldContain "hello"
        w.toString() shouldContain "[?1049l"
    }

    "Writer.alternateScreen restores main screen even when the block throws" {
        val w = StringWriter()
        var rethrown = false
        try {
            w.alternateScreen {
                it.execute(Print("about to fail"))
                error("simulated")
            }
        } catch (_: IllegalStateException) {
            rethrown = true
        }
        rethrown shouldBe true
        w.toString() shouldContain "[?1049h"
        w.toString() shouldContain "[?1049l"
    }

    "Writer.synchronizedUpdate brackets the block with DEC mode 2026" {
        val w = StringWriter()
        w.synchronizedUpdate {
            it.execute(Print("frame"))
        }
        w.toString() shouldContain "[?2026h"
        w.toString() shouldContain "frame"
        w.toString() shouldContain "[?2026l"
    }
})
