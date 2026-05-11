package tools.konsole.rich.progress

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class ProgressTest : StringSpec({

    "addTask creates a started task" {
        val c = Console(terminal = null, writer = StringWriter(), width = 80, colorSystem = ColorSystem.None)
        val p = Progress(c)
        val id = p.addTask("download", total = 100.0)
        val t = p.task(id)
        t.completed shouldBe 0.0
        t.total shouldBe 100.0
        t.started shouldBe true
    }

    "advance increments completed" {
        val c = Console(terminal = null, writer = StringWriter(), width = 80, colorSystem = ColorSystem.None)
        val p = Progress(c)
        val id = p.addTask("x", total = 10.0)
        p.advance(id, 3.0)
        p.task(id).completed shouldBe 3.0
        p.advance(id, 2.0)
        p.task(id).completed shouldBe 5.0
    }

    "task auto-finishes when reaching total" {
        val c = Console(terminal = null, writer = StringWriter(), width = 80, colorSystem = ColorSystem.None)
        val p = Progress(c)
        val id = p.addTask("x", total = 10.0)
        p.advance(id, 10.0)
        p.task(id).finished shouldBe true
    }

    "Progress renders task rows" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val p = Progress(c)
        p.addTask("alpha", total = 100.0, completed = 50.0)
        p.addTask("beta", total = 200.0, completed = 100.0)
        c.print(p)
        val out = sw.toString()
        out shouldContain "alpha"
        out shouldContain "beta"
        out shouldContain "50.0%"
    }

    "update changes description and total" {
        val c = Console(terminal = null, writer = StringWriter(), width = 80, colorSystem = ColorSystem.None)
        val p = Progress(c)
        val id = p.addTask("old", total = 10.0)
        p.update(id, total = 50.0, description = "new")
        p.task(id).total shouldBe 50.0
    }

    "use { } starts the display so advance actually paints" {
        // Regression for the LiveProgress demo: AutoCloseable.use only
        // calls close()/stop(), so `progress.use { ... }` left the Live
        // display un-started and `advance` became a no-op (Live.refresh
        // bails when !started). Progress.use must start before the block
        // and stop after.
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val p = Progress(c, refreshPerSecond = 100.0)
        p.use {
            val id = it.addTask("alpha", total = 10.0)
            it.advance(id, 5.0)
        }
        // Live drew at least the initial frame on start(); the rendered
        // output must contain the task description.
        sw.toString() shouldContain "alpha"
    }
})
