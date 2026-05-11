package tools.konsole.examples

import tools.konsole.core.Terminal

/**
 * Bypass the App layer entirely: enter raw mode and echo every byte JLine
 * reads from the terminal. Use to verify the input path independently of
 * our event/binding stack.
 *
 *   ./demo.sh KeyEcho
 *
 * Press `q` to quit. Ctrl-C also quits.
 */
public fun main() {
    val term = Terminal.system()
    val jline = term.underlying
    System.err.println("[KeyEcho] terminal type=${jline.type} size=${jline.size}")
    val saved = jline.enterRawMode()
    try {
        val reader = jline.reader()
        System.err.println("[KeyEcho] entered raw mode; press keys (q or Ctrl-C to quit)…")
        while (true) {
            val b = reader.read()
            if (b < 0) {
                System.err.println("[KeyEcho] reader returned EOF (-1); exiting")
                break
            }
            val ch = b.toChar()
            val printable = if (b in 32..126) "'$ch'" else "<non-print>"
            System.err.println("[KeyEcho] read byte 0x%02x (%d) %s".format(b, b, printable))
            if (b == 'q'.code || b == 3 /* Ctrl-C */) {
                System.err.println("[KeyEcho] quit byte received; exiting")
                break
            }
        }
    } finally {
        jline.attributes = saved
        term.close()
    }
}
