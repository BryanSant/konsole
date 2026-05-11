package tools.konsole.rich.spinner

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class SpinnerTest : StringSpec({

    "frame advances with time" {
        val s = Spinner("dots", text = tools.konsole.rich.Text("loading"))
        // interval = 80; at t=0 frame[0], at t=80 frame[1], at t=160 frame[2]
        s.frame(0L) shouldBe "⠋"
        s.frame(80L) shouldBe "⠙"
        s.frame(160L) shouldBe "⠹"
    }

    "frame wraps modulo length" {
        val s = Spinner("dots")
        // 10 frames * 80 = 800ms cycle
        s.frame(0L) shouldBe s.frame(800L)
        s.frame(40L) shouldBe s.frame(840L)
    }

    "renders frame plus text" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Spinner("line", text = tools.konsole.rich.Text("hi")))
        val out = sw.toString()
        out shouldContain "hi"
    }

    "Spinner.of accepts markup text" {
        val s = Spinner.of("dots", text = "[bold]loading[/]")
        s.frames.size shouldBe 10
    }

    "unknown spinner throws" {
        runCatching { Spinner("notarealspinner") }.isFailure shouldBe true
    }
})
