package tools.konsole.rich.progress

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import tools.konsole.rich.Console
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Text
import tools.konsole.rich.progress.columns.TaskProgressColumn
import tools.konsole.core.ColorSystem

private fun renderText(task: Task, column: TaskProgressColumn): String {
    val r = column.render(task)
    val console = Console(
        terminal = null,
        writer = java.io.StringWriter(),
        width = 80,
        colorSystem = ColorSystem.None,
    )
    val opts = RenderOptions(maxWidth = 80)
    return r.render(console, opts).joinToString(separator = "") { it.text }
}

private fun task(completed: Double = 0.0, total: Double? = 100.0): Task =
    Task(id = 1, description = Text("x"), total = total, completed = completed)

class TaskProgressColumnTest : StringSpec({

    "renders integer percentage with rich's default format" {
        renderText(task(completed = 42.0), TaskProgressColumn()) shouldBe " 42%"
    }

    "renders 100% when complete" {
        renderText(task(completed = 100.0), TaskProgressColumn()) shouldBe "100%"
    }

    "renders empty when total is null and showSpeed is false" {
        renderText(task(total = null), TaskProgressColumn()) shouldBe ""
    }

    "renders 'X.X it/s' when total is null and showSpeed is true with measurable speed" {
        // Set startTime far enough in the past for elapsed > 0
        val t = task(completed = 5.0, total = null)
        // reflectively set startTime — internal setter only
        val field = Task::class.java.getDeclaredField("startTime")
        field.isAccessible = true
        field.set(t, System.nanoTime() - 1_000_000_000L) // 1 second ago

        val out = renderText(t, TaskProgressColumn(showSpeed = true))
        out shouldEndWith " it/s"
    }
})
