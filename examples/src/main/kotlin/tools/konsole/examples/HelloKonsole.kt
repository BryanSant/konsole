package tools.konsole.examples

import tools.konsole.core.Ansi
import tools.konsole.core.Terminal

/**
 * Phase 0 smoke test: open a system terminal via konsole's wrapper, print
 * "hello konsole" in bold cyan via [Ansi.CSI], then reset.
 *
 *   ./demo.sh HelloKonsole
 */
public fun main() {
    Terminal.system().use { terminal ->
        val w = terminal.out
        // SGR: bold (1) + cyan-fg (36) → reset (0)
        w.append("${Ansi.CSI}1;36m")
        w.append("hello konsole")
        w.append("${Ansi.CSI}0m")
        w.append('\n')
        w.append("terminal type: ${terminal.tty.typeLabel}\n")
        val size = terminal.size
        w.append("size: ${size.columns} cols x ${size.rows} rows\n")
        w.flush()
    }
}
