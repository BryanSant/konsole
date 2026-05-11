package tools.konsole.rich.progress

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.live.Live
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.progress.columns.BarColumn
import tools.konsole.rich.progress.columns.MofNCompleteColumn
import tools.konsole.rich.progress.columns.PercentageColumn
import tools.konsole.rich.progress.columns.ProgressColumn
import tools.konsole.rich.progress.columns.TaskDescriptionColumn
import tools.konsole.rich.progress.columns.TextColumn
import tools.konsole.rich.progress.columns.TimeRemainingColumn
import tools.konsole.rich.table.Table
import tools.konsole.rich.text.Justify
import java.util.concurrent.atomic.AtomicInteger

/**
 * A live multi-task progress display. Mirrors `rich.progress.Progress`.
 *
 * Usage:
 *   ```
 *   Progress(console).use { progress ->
 *       val task1 = progress.addTask("download", total = 100.0)
 *       val task2 = progress.addTask("process", total = 50.0)
 *       repeat(100) { progress.advance(task1) ; ... }
 *   }
 *   ```
 */
public class Progress(
    public val console: Console,
    public val columns: List<ProgressColumn> = defaultColumns(),
    public val refreshPerSecond: Double = 10.0,
    public val transient: Boolean = false,
    public val expand: Boolean = false,
) : AutoCloseable, Renderable {

    private val tasks: MutableMap<Int, Task> = linkedMapOf()
    private val nextId = AtomicInteger(0)
    private val live: Live = Live(this, console, refreshPerSecond = refreshPerSecond, transient = transient)
    private var started = false

    public val taskIds: List<Int> get() = tasks.keys.toList()
    public fun task(id: Int): Task = tasks[id] ?: error("Unknown task id: $id")

    /** Add a task and return its id. The task is automatically started. */
    public fun addTask(
        description: String,
        total: Double? = 100.0,
        completed: Double = 0.0,
        visible: Boolean = true,
        start: Boolean = true,
    ): Int {
        val id = nextId.getAndIncrement()
        val task = Task(
            id = id,
            description = Markup.parse(description),
            total = total,
            completed = completed,
            visible = visible,
        )
        if (start) task.startTime = System.nanoTime()
        tasks[id] = task
        if (started) live.refresh()
        return id
    }

    /** Manually start a task (sets startTime). */
    public fun startTask(id: Int) {
        val t = task(id)
        if (t.startTime == null) t.startTime = System.nanoTime()
        if (started) live.refresh()
    }

    /** Manually mark a task as finished (stops the elapsed clock). */
    public fun stopTask(id: Int) {
        val t = task(id)
        if (t.stopTime == null) t.stopTime = System.nanoTime()
        if (started) live.refresh()
    }

    /** Increment [Task.completed] by [advance]. Triggers a redraw. */
    public fun advance(id: Int, advance: Double = 1.0) {
        val t = task(id)
        t.completed += advance
        if (t.total != null && t.completed >= (t.total ?: 0.0) && t.stopTime == null) {
            t.stopTime = System.nanoTime()
        }
        if (started) live.refresh()
    }

    /** Update arbitrary task fields. */
    public fun update(
        id: Int,
        completed: Double? = null,
        total: Double? = null,
        description: String? = null,
        visible: Boolean? = null,
    ) {
        val t = task(id)
        if (completed != null) t.completed = completed
        if (total != null) t.total = total
        if (description != null) t.description = Markup.parse(description)
        if (visible != null) t.visible = visible
        if (started) live.refresh()
    }

    public fun start() {
        if (started) return
        started = true
        live.start()
    }

    public fun stop() {
        if (!started) return
        started = false
        live.stop()
    }

    override fun close() {
        stop()
    }

    /** Convenience: track an iterable; auto-advances per element. */
    public fun <T> track(
        items: Iterable<T>,
        description: String = "Working...",
        total: Double? = null,
    ): Sequence<T> {
        val effectiveTotal = total ?: (items as? Collection<T>)?.size?.toDouble()
        val id = addTask(description, total = effectiveTotal)
        return sequence {
            try {
                for (item in items) {
                    yield(item)
                    advance(id, 1.0)
                }
            } finally {
                stopTask(id)
            }
        }
    }

    /** Renders all visible tasks as a table-like grid. */
    override fun render(console: Console, options: RenderOptions): Sequence<Segment> {
        val grid = Table(box = null, showEdge = false, showHeader = false, padding = tools.konsole.rich.layout.Padding(0, 1, 0, 0))
        for (i in columns.indices) {
            // Each column auto-sizes to its widest cell.
            val justify = if (columns[i] is tools.konsole.rich.progress.columns.PercentageColumn) Justify.Right else Justify.Left
            grid.addColumn(tools.konsole.rich.table.Column(justify = justify))
        }
        for (task in tasks.values) {
            if (!task.visible) continue
            val cells = columns.map { it.render(task) }
            grid.addRow(*cells.toTypedArray())
        }
        return grid.render(console, options)
    }

    public companion object {
        public fun defaultColumns(): List<ProgressColumn> = listOf(
            TaskDescriptionColumn(),
            BarColumn(),
            PercentageColumn(),
            TextColumn("•"),
            MofNCompleteColumn(),
            TextColumn("•"),
            TimeRemainingColumn(),
        )
    }
}

/**
 * Convenience: wrap an [Iterable] with a transient progress bar.
 */
public fun <T> Console.track(
    items: Iterable<T>,
    description: String = "Working...",
    total: Double? = null,
): Sequence<T> {
    val total2 = total ?: (items as? Collection<T>)?.size?.toDouble()
    val progress = Progress(this, transient = true)
    progress.start()
    val id = progress.addTask(description, total = total2)
    return sequence {
        try {
            for (item in items) {
                yield(item)
                progress.advance(id, 1.0)
            }
        } finally {
            progress.stopTask(id)
            progress.stop()
        }
    }
}
