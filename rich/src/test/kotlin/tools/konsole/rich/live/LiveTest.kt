package tools.konsole.rich.live

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.StringWriter
import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Text

class LiveTest : StringSpec({

    "drawFrame overwrites rows instead of clearing the whole region (no flicker)" {
        // The pre-fix pattern was Clear(FromCursorDown) followed by
        // re-emitting every row — which leaves the region briefly empty.
        // The new pattern should never emit a full-screen clear (CSI J or
        // CSI 0 J). Per-row tail clears (CSI K) are fine and expected.
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 40, colorSystem = ColorSystem.None)
        val live = Live(Text("line one"), c, autoRefresh = false)
        live.start()
        live.update(Text("line one"))   // identical content
        live.update(Text("line two"))   // changed
        live.stop()
        val output = sw.toString()
        // ESC [ J  (no parameter) = Clear FromCursorDown. We must never emit this
        // during steady-state redraws — that's the flicker source.
        output shouldNotContain "[J"
        // The new content must appear.
        output shouldContain "line two"
    }

    "drawFrame skips unchanged rows on the second redraw" {
        // When the renderable produces the same lines as the prior frame
        // the diff must skip them — the writer should not see those
        // segments repeated.
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 40, colorSystem = ColorSystem.None)
        val live = Live(Text("unchanging-row-abc"), c, autoRefresh = false)
        live.start()
        val beforeRefresh = sw.toString().length
        live.refresh()  // same content
        val afterRefresh = sw.toString().length
        live.stop()
        // The second draw should emit far fewer bytes — at most cursor
        // moves, no repeated row content. Specifically the "unchanging"
        // string should appear exactly once in the captured output.
        sw.toString().split("unchanging-row-abc").size - 1 shouldBe 1
        // And the byte delta from the second redraw should be small
        // (cursor positioning only).
        (afterRefresh - beforeRefresh < 30) shouldBe true
    }
})
