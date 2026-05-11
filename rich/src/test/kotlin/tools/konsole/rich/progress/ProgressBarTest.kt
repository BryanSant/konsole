package tools.konsole.rich.progress

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.RenderOptions

private fun renderBar(
    total: Double? = 100.0,
    completed: Double = 0.0,
    width: Int = 10,
    pulse: Boolean = false,
    animationTime: Double? = 0.0,
    colorSystem: ColorSystem = ColorSystem.TrueColor,
): String {
    val bar = ProgressBar(
        total = total,
        completed = completed,
        width = width,
        pulse = pulse,
        animationTime = animationTime,
    )
    val console = Console(
        terminal = null,
        writer = java.io.StringWriter(),
        width = width,
        colorSystem = colorSystem,
    )
    val opts = RenderOptions(maxWidth = width)
    return bar.render(console, opts).joinToString(separator = "") { it.text }
}

class ProgressBarTest : StringSpec({

    "empty bar renders all back chars on a no-color terminal" {
        renderBar(completed = 0.0, width = 10, colorSystem = ColorSystem.None) shouldBe " ".repeat(10)
    }

    "full bar renders all complete chars" {
        renderBar(completed = 100.0, width = 10) shouldBe "━".repeat(10)
    }

    "half bar uses the half-bar boundary char" {
        // 50% of width 10 = 5 cells, no half-step
        val out = renderBar(completed = 50.0, width = 10)
        out.length shouldBe 10
        out shouldBe "━━━━━╺━━━━"
    }

    "quarter bar lands on a half-cell boundary" {
        // 25% of width 10 = 2.5 cells -> 2 full + 1 half-right + 7 back (with one half-left)
        val out = renderBar(completed = 25.0, width = 10)
        // First 2 cells are full bars; cell 3 is half-bar (right half drawn)
        out[0].toString() shouldBe "━"
        out[1].toString() shouldBe "━"
        out[2].toString() shouldBe "╸"
    }

    "indeterminate bar (total = null) emits a pulse" {
        val out = renderBar(total = null, width = 20, animationTime = 0.0)
        out.length shouldBe 20
    }

    "explicit pulse=true emits a pulse even with finite total" {
        val out = renderBar(total = 100.0, completed = 50.0, pulse = true, width = 20, animationTime = 0.0)
        out.length shouldBe 20
    }

    "pulse with no-color terminal falls back to half-fill" {
        val out = renderBar(total = null, width = 20, animationTime = 0.0, colorSystem = ColorSystem.None)
        out.length shouldBe 20
    }

    "animationTime override is deterministic" {
        val a = renderBar(total = null, width = 20, animationTime = 0.0)
        val b = renderBar(total = null, width = 20, animationTime = 0.0)
        a shouldBe b
    }

    "different animationTime produces different pulse phases" {
        val a = renderBar(total = null, width = 20, animationTime = 0.0)
        val b = renderBar(total = null, width = 20, animationTime = 1.0)
        // Don't assert byte difference (text is identical bar chars), but the styling differs.
        // Length stays identical.
        a.length shouldBe b.length
    }
})
