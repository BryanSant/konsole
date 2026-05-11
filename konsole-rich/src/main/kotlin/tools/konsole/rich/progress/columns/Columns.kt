package tools.konsole.rich.progress.columns

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.progress.Task
import tools.konsole.rich.spinner.Spinner

/** Renders the task description (a Renderable). */
public class TaskDescriptionColumn(public val style: Style? = null) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val r = task.description
        return r
    }
}

/** Static markup text — useful as a separator. */
public class TextColumn(public val text: String) : ProgressColumn {
    override fun render(task: Task): Renderable = Markup.parse(text)
}

/** Renderable computed per task by a user lambda. */
public class RenderableColumn(public val function: (Task) -> Renderable) : ProgressColumn {
    override fun render(task: Task): Renderable = function(task)
}

/**
 * Bar visualising percent complete. Mirrors rich's `BarColumn`.
 *
 * Defaults to a fixed [barWidth] of 40 cells; pass `null` to flex with the row.
 * Pulse animation engages automatically when a task has `total = null` (or when
 * [pulse] is forced on).
 */
public class BarColumn(
    public val barWidth: Int? = 40,
    public val style: Style = Style(color = tools.konsole.core.style.Color.Rgb(58, 58, 58)),
    public val completeStyle: Style = Style(color = tools.konsole.core.style.Color.Rgb(249, 38, 114)),
    public val finishedStyle: Style = Style(color = tools.konsole.core.style.Color.Rgb(40, 196, 41)),
    public val pulseStyle: Style = Style(color = tools.konsole.core.style.Color.Rgb(174, 199, 232)),
    public val pulse: Boolean = false,
) : ProgressColumn {
    override fun render(task: Task): Renderable = tools.konsole.rich.progress.ProgressBar(
        total = task.total,
        completed = task.completed,
        width = barWidth,
        pulse = pulse,
        style = style,
        completeStyle = completeStyle,
        finishedStyle = finishedStyle,
        pulseStyle = pulseStyle,
    )
}

/** Numeric percentage column (e.g. " 42.0%"). Konsole extension; rich uses [TaskProgressColumn]. */
public class PercentageColumn(public val style: Style = Style(color = tools.konsole.core.style.Color.Magenta)) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val p = task.percentage
        val text = if (p == null) "  ?  %" else String.format("%5.1f%%", p)
        return Text(text, style = style)
    }
}

/**
 * Show task progress as a percentage. Mirrors rich's `TaskProgressColumn` —
 * formats `task.percentage` with the rich-default `{:>3.0f}%` (e.g. " 42%"),
 * and optionally falls back to a speed display when `total` is unknown.
 *
 * @param style style applied to the rendered text. Defaults to the theme's
 *   `progress.percentage` look (magenta) — matches rich.
 * @param showSpeed when total is unknown, render `task.speed` as `42.0 it/s` instead of `""`.
 *   Mirrors `show_speed` in rich.
 */
public class TaskProgressColumn(
    public val style: Style = Style(color = tools.konsole.core.style.Color.Magenta),
    public val showSpeed: Boolean = false,
) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val p = task.percentage
        if (p != null) {
            return Text(String.format("%3d%%", p.toInt()), style = style)
        }
        if (showSpeed) {
            val s = task.speed
            return Text(if (s == null) "" else String.format("%.1f it/s", s), style = style)
        }
        return Text("", style = style)
    }
}

/** "M/N" completion column. */
public class MofNCompleteColumn(public val style: Style? = null) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val total = task.total
        val text = if (total == null) "${task.completed.toLong()}/?" else "${task.completed.toLong()}/${total.toLong()}"
        return Text(text, style = style ?: Style.NULL)
    }
}

/** Elapsed time as `H:MM:SS`. */
public class TimeElapsedColumn(public val style: Style = Style(color = tools.konsole.core.style.Color.Yellow)) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val e = task.elapsed
        return Text(if (e == null) "-:--:--" else formatHMS(e.toLong()), style = style)
    }
}

/** Estimated time remaining as `H:MM:SS`. */
public class TimeRemainingColumn(public val style: Style = Style(color = tools.konsole.core.style.Color.Cyan)) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val r = task.timeRemaining
        return Text(if (r == null) "-:--:--" else formatHMS(r.toLong()), style = style)
    }
}

/** Animated spinner column; finished tasks show a check mark. */
public class SpinnerColumn(
    public val spinnerName: String = "dots",
    public val finishedText: String = "✓",
    public val style: Style = Style(color = tools.konsole.core.style.Color.Green),
) : ProgressColumn {
    override fun render(task: Task): Renderable {
        if (task.finished) return Text(finishedText, style = style)
        return Spinner(spinnerName, style = style)
    }
}

/** File-size column — formats `task.completed` as bytes. */
public class FileSizeColumn(public val style: Style = Style(color = tools.konsole.core.style.Color.Green)) : ProgressColumn {
    override fun render(task: Task): Renderable = Text(formatBytes(task.completed.toLong()), style = style)
}

/** Total file size column — formats `task.total` as bytes. */
public class TotalFileSizeColumn(public val style: Style = Style(color = tools.konsole.core.style.Color.Green)) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val t = task.total ?: return Text("?", style = style)
        return Text(formatBytes(t.toLong()), style = style)
    }
}

/** "completed/total" file-size combo. */
public class DownloadColumn(public val style: Style = Style(color = tools.konsole.core.style.Color.Green)) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val total = task.total
        val text = if (total == null) "${formatBytes(task.completed.toLong())} / ?"
        else "${formatBytes(task.completed.toLong())} / ${formatBytes(total.toLong())}"
        return Text(text, style = style)
    }
}

/** Transfer-speed column — bytes per second. */
public class TransferSpeedColumn(public val style: Style = Style(color = tools.konsole.core.style.Color.Red)) : ProgressColumn {
    override fun render(task: Task): Renderable {
        val s = task.speed ?: return Text("?", style = style)
        return Text("${formatBytes(s.toLong())}/s", style = style)
    }
}

internal fun formatHMS(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%d:%02d:%02d".format(h, m, s)
}

internal fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "kB", "MB", "GB", "TB", "PB")
    var v = bytes.toDouble()
    var u = 0
    while (v >= 1024.0 && u < units.lastIndex) {
        v /= 1024.0
        u += 1
    }
    return if (u == 0) "${bytes} ${units[u]}" else "%.1f %s".format(v, units[u])
}
