package tools.konsole.rich.progress.columns

import tools.konsole.rich.Renderable
import tools.konsole.rich.progress.Task

/** Renders a single column for one [Task]. Mirrors `rich.progress.ProgressColumn`. */
public fun interface ProgressColumn {
    public fun render(task: Task): Renderable
}
