package tools.konsole.examples

import tools.konsole.core.Ansi
import tools.konsole.core.Terminal

/**
 * Phase 0 smoke test: open a system terminal via konsole's wrapper, print
 * "hello konsole" in bold cyan via [Ansi.CSI], then reset.
 *
 *   ./demo.sh HelloKonsole
 *
 * Uses [Terminal.system] rather than `TerminalBuilder.builder().system(true)`
 * directly so the konsole workarounds for JLine 4.1.0 quirks (SIGINFO NPE on
 * Linux, grapheme-cluster probe CPR leak, raw-mode termios) all kick in. If
 * you call JLine's builder yourself you get the unworkaround'd behaviour and
 * end up with a dumb terminal at size 0×0.
 */
public fun main() {
    Terminal.system().use { terminal ->
        val jline = terminal.underlying
        val w = jline.writer()
        // SGR: bold (1) + cyan-fg (36) → reset (0)
        w.print("${Ansi.CSI}1;36m")
        w.print("hello konsole")
        w.print("${Ansi.CSI}0m")
        w.println()
        w.println("terminal type: ${jline.type}")
        w.println("size: ${jline.size.columns} cols x ${jline.size.rows} rows")
        w.flush()
    }
}
