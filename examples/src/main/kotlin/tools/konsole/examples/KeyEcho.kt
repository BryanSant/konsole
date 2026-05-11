package tools.konsole.examples

import tools.konsole.core.InputMode
import tools.konsole.core.Terminal
import tools.konsole.core.event.AnsiInputParser
import tools.konsole.core.event.Event
import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyEventKind

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
    val term = Terminal.system()
    val jline = term.underlying
    System.err.println("[KeyEcho] terminal=${jline.type} size=${jline.size}")
    val saved = jline.enterRawMode()
    Terminal.fixupBlockingRawMode(jline)

    val sbEnable = StringBuilder()
    InputMode.EnableAll(kitty = true, mouseMotion = false).writeAnsi(sbEnable)
    jline.writer().print(sbEnable.toString())
    jline.writer().flush()
    System.err.println("[KeyEcho] entered raw mode + kitty / SGR mouse / paste / focus / resize.")
    System.err.println("[KeyEcho] press keys; 'q' or Ctrl-C quits. (Both presses and releases are reported.)")

    val parser = AnsiInputParser()
    val reader = jline.reader()
    try {
        loop@ while (true) {
            val b = reader.read()
            if (b < 0) {
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
        val sbDisable = StringBuilder()
        InputMode.DisableAll(kitty = true, mouseMotion = false).writeAnsi(sbDisable)
        jline.writer().print(sbDisable.toString())
        jline.writer().flush()
    } finally {
        jline.attributes = saved
        term.close()
    }
}
