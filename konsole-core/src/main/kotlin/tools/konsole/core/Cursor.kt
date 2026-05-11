package tools.konsole.core

import tools.konsole.core.Ansi.CSI
import tools.konsole.core.Ansi.ESC

// ---- Position ----

/**
 * A cursor position: zero-indexed (column, row).
 *
 * Stored as a packed Long for allocation-free returns.
 */
@JvmInline
public value class Position internal constructor(@PublishedApi internal val packed: Long) {
    public val column: Int get() = (packed ushr 32).toInt()
    public val row: Int get() = (packed and 0xFFFFFFFFL).toInt()

    public operator fun component1(): Int = column
    public operator fun component2(): Int = row

    override fun toString(): String = "Position(column=$column, row=$row)"

    public companion object {
        public fun of(column: Int, row: Int): Position {
            require(column >= 0 && row >= 0) { "Position must be non-negative: ($column, $row)" }
            return Position((column.toLong() shl 32) or (row.toLong() and 0xFFFFFFFFL))
        }
    }
}

// ---- Style ----

/**
 * Cursor shape variants, mapped to the DECSCUSR control codes (`CSI N q`).
 */
public enum class CursorStyle(internal val code: Int) {
    DefaultUserShape(0),
    BlinkingBlock(1),
    SteadyBlock(2),
    BlinkingUnderscore(3),
    SteadyUnderscore(4),
    BlinkingBar(5),
    SteadyBar(6),
}

// ---- Movement ----

/** Move to absolute (column, row). Both are 0-indexed; ANSI uses 1-indexed and we adjust. */
public data class MoveTo(val column: Int, val row: Int) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append((row + 1).toString()).append(';').append((column + 1).toString()).append('H')
    }
}

public data class MoveToColumn(val column: Int) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append((column + 1).toString()).append('G')
    }
}

public data class MoveToRow(val row: Int) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append((row + 1).toString()).append('d')
    }
}

public data class MoveToNextLine(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('E')
    }
}

public data class MoveToPreviousLine(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('F')
    }
}

public data class MoveUp(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('A')
    }
}

public data class MoveDown(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('B')
    }
}

public data class MoveRight(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('C')
    }
}

public data class MoveLeft(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('D')
    }
}

// ---- Save/restore (DECSC / DECRC) ----

public data object SavePosition : Command {
    override fun writeAnsi(out: Appendable) { out.append(ESC).append('7') }
}

public data object RestorePosition : Command {
    override fun writeAnsi(out: Appendable) { out.append(ESC).append('8') }
}

// ---- Visibility ----

public data object Hide : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?25l") }
}

public data object Show : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?25h") }
}

// ---- Blinking ----

public data object EnableBlinking : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?12h") }
}

public data object DisableBlinking : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?12l") }
}

// ---- Shape ----

public data class SetCursorStyle(val style: CursorStyle) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append(style.code.toString()).append(" q")
    }
}
