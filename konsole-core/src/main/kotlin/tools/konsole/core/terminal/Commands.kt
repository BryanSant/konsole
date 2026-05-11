package tools.konsole.core.terminal

import tools.konsole.core.Command
import tools.konsole.core.Ansi.CSI
import tools.konsole.core.Ansi.OSC
import tools.konsole.core.Ansi.ST

// ---- Clear ----

public data class Clear(val type: ClearType) : Command {
    override fun writeAnsi(out: Appendable) {
        when (type) {
            ClearType.All -> out.append(CSI).append("2J")
            ClearType.Purge -> out.append(CSI).append("3J")
            ClearType.FromCursorDown -> out.append(CSI).append("J")
            ClearType.FromCursorUp -> out.append(CSI).append("1J")
            ClearType.CurrentLine -> out.append(CSI).append("2K")
            ClearType.UntilNewLine -> out.append(CSI).append("K")
        }
    }
}

// ---- Scroll ----

public data class ScrollUp(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('S')
    }
}

public data class ScrollDown(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('T')
    }
}

// ---- Resize / title ----

public data class SetSize(val columns: Int, val rows: Int) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append("8;").append(rows.toString()).append(';').append(columns.toString()).append('t')
    }
}

public data class SetTitle(val title: String) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("0;").append(title).append(ST)
    }
}

// ---- Alternate screen ----

public data object EnterAlternateScreen : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1049h") }
}

public data object LeaveAlternateScreen : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?1049l") }
}

// ---- Synchronized output (DEC mode 2026) ----

public data object BeginSynchronizedUpdate : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2026h") }
}

public data object EndSynchronizedUpdate : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?2026l") }
}

// ---- Line wrap ----

public data object EnableLineWrap : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?7h") }
}

public data object DisableLineWrap : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?7l") }
}
