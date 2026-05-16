package tools.konsole.examples

import tools.konsole.core.InputMode
import tools.konsole.core.Terminal
import tools.konsole.core.event.AnsiInputParser
import tools.konsole.core.event.Event
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyEventKind
import tools.konsole.core.tty.Tty

/**
 * Enter raw mode + the App's input-mode set (kitty / SGR mouse / bracketed
 * paste / focus / in-band resize), then feed every byte to konsole's
 * `AnsiInputParser` and print the parsed events. Useful as a key-binding
 * debugger and to verify the parser handles whatever the terminal sends
 * in the modes our App turns on.
 *
 *   ./demo.sh KeyEcho
 *
 * Press `q` to quit. Ctrl-C also quits.
 */
public fun main() {
    Terminal.system().use { term ->
        val tty = term.tty
        val size = term.size
        System.err.println("[KeyEcho] terminal=${tty.typeLabel} size=${size.columns}x${size.rows}")

        term.rawMode {
            val sbEnable = StringBuilder()
            InputMode.EnableAll(kitty = true, mouseMotion = false).writeAnsi(sbEnable)
            tty.out.append(sbEnable)
            tty.out.flush()
            System.err.println("[KeyEcho] entered raw mode + kitty / SGR mouse / paste / focus / resize.")
            System.err.println("[KeyEcho] press keys; 'q' or Ctrl-C quits. (Both presses and releases are reported.)")

            val parser = AnsiInputParser()
            try {
                loop@ while (true) {
                    val b = tty.read()
                    if (b == Tty.EOF) {
                        System.err.println("[KeyEcho] reader EOF; exiting")
                        break
                    }
                    val ev = parser.advance(b) ?: continue
                    System.err.println("[KeyEcho] $ev")
                    if (ev is Event.Key) {
                        val ke = ev.event
                        // Only quit on PRESS (not release/repeat) so we don't react to the
                        // companion release that arrives with REPORT_EVENT_TYPES enabled.
                        if (ke.kind == KeyEventKind.Press) {
                            val code = ke.code
                            if (code is KeyCode.Char && code.c == 'q' && !ke.modifiers.hasControl()) break@loop
                            if (code is KeyCode.Char && code.c == 'c' && ke.modifiers.hasControl()) break@loop
                        }
                    }
                }
                System.err.println("[KeyEcho] quitting; restoring terminal…")
            } finally {
                val sbDisable = StringBuilder()
                InputMode.DisableAll(kitty = true, mouseMotion = false).writeAnsi(sbDisable)
                tty.out.append(sbDisable)
                tty.out.flush()
            }
        }
    }
}
