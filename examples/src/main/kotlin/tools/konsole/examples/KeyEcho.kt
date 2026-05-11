package tools.konsole.examples

import tools.konsole.core.InputMode
import tools.konsole.core.Terminal
import java.io.FileDescriptor
import java.io.FileInputStream

/**
 * Bypass the App layer entirely: enter raw mode + kitty/SGR-mouse/bracketed-
 * paste/focus/in-band-resize (the same input modes our App enables), then
 * echo every byte JLine reads from the terminal. Tells us exactly what the
 * terminal sends in the same mode our App runs in, isolated from our parser
 * and event stack.
 *
 *   ./demo.sh KeyEcho
 *
 * Press `q` followed by Enter to quit. (With REPORT_ALL_KEYS_AS_ESCAPE_CODES,
 * `q` may come through as `ESC [ 113 u` rather than a bare `0x71`, so we
 * accept either form for the quit signal.)
 */
public fun main() {
    val term = Terminal.system()
    val jline = term.underlying
    System.err.println("[KeyEcho] terminal type=${jline.type} size=${jline.size}")
    val saved = jline.enterRawMode()
    Terminal.fixupBlockingRawMode(jline)
    try {
        // Write the same enable-modes our App uses, so we capture the bytes
        // ghostty/wezterm/etc. actually emit in that mode.
        val sb = StringBuilder()
        InputMode.EnableAll(kitty = true, mouseMotion = false).writeAnsi(sb)
        jline.writer().print(sb.toString())
        jline.writer().flush()
        // We deliberately bypass JLine's NonBlockingReader and read from
        // FileDescriptor.in directly. The previous run showed JLine returning
        // EOF immediately after the terminal's focus-in + resize responses;
        // this probe rules in/out whether that's a JLine wrapper artefact
        // vs. an actual EOF from the kernel.
        val rawIn = FileInputStream(FileDescriptor.`in`)
        System.err.println("[KeyEcho] entered raw mode + kitty/SGR-mouse/paste/focus/resize.")
        System.err.println("[KeyEcho] reading FROM FileDescriptor.in DIRECTLY (bypassing JLine).")
        System.err.println("[KeyEcho] press keys; type 'q' to quit (or send 0x03 Ctrl-C)…")
        val seenBytes = StringBuilder()
        while (true) {
            val b = rawIn.read()
            if (b < 0) {
                System.err.println("[KeyEcho] FileDescriptor.in returned $b (true kernel EOF); exiting")
                break
            }
            val ch = b.toChar()
            val printable = if (b in 32..126) "'$ch'" else if (b == 0x1B) "<ESC>" else "<non-print>"
            System.err.println("[KeyEcho] read byte 0x%02x (%d) %s".format(b, b, printable))
            seenBytes.append(b.toChar())
            // Quit on a bare 'q', a CSI-u 'q' tail "[113u", or Ctrl-C (0x03).
            if (b == 3) break
            val tail = seenBytes.takeLast(8).toString()
            if (b == 'q'.code && tail.length == 1) break
            if (tail.endsWith("[113u")) break
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
