package tools.konsole.examples

import org.jline.terminal.TerminalBuilder
import tools.konsole.core.Ansi

/**
 * Phase 0 smoke test: open a JLine system terminal via FFM, print
 * "hello konsole" in bold cyan via [Ansi.CSI], then reset.
 *
 * Run with:
 *   ./gradlew :examples:runExample -Pexample=HelloKonsole
 */
public fun main() {
    val terminal = TerminalBuilder.builder()
        .system(true)
        .build()
    try {
        val w = terminal.writer()
        // SGR: bold (1) + cyan-fg (36) → reset (0)
        w.print("${Ansi.CSI}1;36m")
        w.print("hello konsole")
        w.print("${Ansi.CSI}0m")
        w.println()
        w.println("terminal type: ${terminal.type}")
        w.println("size: ${terminal.size.columns} cols x ${terminal.size.rows} rows")
        w.flush()
    } finally {
        terminal.close()
    }
}
