package tools.konsole.core

import tools.konsole.core.Hide
import tools.konsole.core.MoveDown
import tools.konsole.core.MoveLeft
import tools.konsole.core.MoveRight
import tools.konsole.core.MoveTo
import tools.konsole.core.MoveToColumn
import tools.konsole.core.MoveToNextLine
import tools.konsole.core.MoveToPreviousLine
import tools.konsole.core.MoveToRow
import tools.konsole.core.MoveUp
import tools.konsole.core.RestorePosition
import tools.konsole.core.SavePosition
import tools.konsole.core.Show
import tools.konsole.core.style.Attribute
import tools.konsole.core.style.Attributes
import tools.konsole.core.style.Color
import tools.konsole.core.style.ContentStyle
import tools.konsole.core.style.Print
import tools.konsole.core.style.ResetColor
import tools.konsole.core.style.SetAttribute
import tools.konsole.core.style.SetAttributes
import tools.konsole.core.style.SetBackgroundColor
import tools.konsole.core.style.SetForegroundColor
import tools.konsole.core.style.SetStyle
import tools.konsole.core.style.SetUnderlineColor
import tools.konsole.core.terminal.Clear
import tools.konsole.core.terminal.ClearType
import tools.konsole.core.terminal.DisableLineWrap
import tools.konsole.core.terminal.EnableLineWrap
import tools.konsole.core.terminal.EnterAlternateScreen
import tools.konsole.core.terminal.LeaveAlternateScreen
import tools.konsole.core.terminal.ScrollDown
import tools.konsole.core.terminal.ScrollUp
import tools.konsole.core.terminal.SetSize
import tools.konsole.core.terminal.SetTitle
import tools.konsole.core.SendNotification
import tools.konsole.core.ProgressState
import tools.konsole.core.SetProgress
import java.io.Writer

/** Restricts implicit receivers so `terminal { terminal { … } }` doesn't compile. */
@DslMarker
public annotation class KrosstermDsl

/**
 * Receiver for the [terminal] DSL. Every Command has a same-named lower-camel
 * shorthand here. For commands not yet shorthanded, use [command] or `+command`.
 */
@KrosstermDsl
public class TerminalScope @PublishedApi internal constructor(
    @PublishedApi internal val out: Writer,
) {
    // ---- Cursor ----
    public fun moveTo(column: Int, row: Int): Unit = MoveTo(column, row).writeAnsi(out)
    public fun moveToColumn(column: Int): Unit = MoveToColumn(column).writeAnsi(out)
    public fun moveToRow(row: Int): Unit = MoveToRow(row).writeAnsi(out)
    public fun moveToNextLine(n: Int = 1): Unit = MoveToNextLine(n).writeAnsi(out)
    public fun moveToPreviousLine(n: Int = 1): Unit = MoveToPreviousLine(n).writeAnsi(out)
    public fun moveUp(n: Int = 1): Unit = MoveUp(n).writeAnsi(out)
    public fun moveDown(n: Int = 1): Unit = MoveDown(n).writeAnsi(out)
    public fun moveLeft(n: Int = 1): Unit = MoveLeft(n).writeAnsi(out)
    public fun moveRight(n: Int = 1): Unit = MoveRight(n).writeAnsi(out)
    public fun savePosition(): Unit = SavePosition.writeAnsi(out)
    public fun restorePosition(): Unit = RestorePosition.writeAnsi(out)
    public fun hide(): Unit = Hide.writeAnsi(out)
    public fun show(): Unit = Show.writeAnsi(out)

    // ---- Terminal ----
    public fun clear(type: ClearType = ClearType.All): Unit = Clear(type).writeAnsi(out)
    public fun scrollUp(n: Int = 1): Unit = ScrollUp(n).writeAnsi(out)
    public fun scrollDown(n: Int = 1): Unit = ScrollDown(n).writeAnsi(out)
    public fun setSize(columns: Int, rows: Int): Unit = SetSize(columns, rows).writeAnsi(out)
    public fun setTitle(title: String): Unit = SetTitle(title).writeAnsi(out)
    public fun setProgress(state: ProgressState): Unit = SetProgress(state).writeAnsi(out)
    public fun sendNotification(title: String, body: String): Unit = SendNotification(title, body).writeAnsi(out)
    public fun enterAlternateScreen(): Unit = EnterAlternateScreen.writeAnsi(out)
    public fun leaveAlternateScreen(): Unit = LeaveAlternateScreen.writeAnsi(out)
    public fun enableLineWrap(): Unit = EnableLineWrap.writeAnsi(out)
    public fun disableLineWrap(): Unit = DisableLineWrap.writeAnsi(out)

    // ---- Style ----
    public fun setForegroundColor(color: Color): Unit = SetForegroundColor(color).writeAnsi(out)
    public fun setBackgroundColor(color: Color): Unit = SetBackgroundColor(color).writeAnsi(out)
    public fun setUnderlineColor(color: Color): Unit = SetUnderlineColor(color).writeAnsi(out)
    public fun setAttribute(attribute: Attribute): Unit = SetAttribute(attribute).writeAnsi(out)
    public fun setAttributes(attributes: Attributes): Unit = SetAttributes(attributes).writeAnsi(out)
    public fun setStyle(style: ContentStyle): Unit = SetStyle(style).writeAnsi(out)
    public fun resetColor(): Unit = ResetColor.writeAnsi(out)

    // ---- Print ----
    public fun print(value: Any?): Unit = Print(value).writeAnsi(out)
    public fun println(value: Any? = ""): Unit { Print(value).writeAnsi(out); out.append('\n') }

    // ---- Escape hatches ----
    public operator fun Command.unaryPlus(): Unit = writeAnsi(out)
    public fun command(c: Command): Unit = c.writeAnsi(out)
    public fun raw(s: String): Unit { out.append(s) }
}

/**
 * Open a [TerminalScope] over this writer. The block is executed against the
 * scope receiver; the writer is flushed on exit (including via exception).
 */
public inline fun <T> Writer.terminal(block: TerminalScope.() -> T): T {
    val scope = TerminalScope(this)
    return try {
        scope.block()
    } finally {
        flush()
    }
}
