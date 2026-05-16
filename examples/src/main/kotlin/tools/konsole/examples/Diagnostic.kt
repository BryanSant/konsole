package tools.konsole.examples

import tools.konsole.core.Terminal
import tools.konsole.core.tty.Stream

/**
 * Dump everything we know about the JVM/terminal connection. When a demo
 * looks broken (renders into 1 column, reports size 0×0, etc.), run this
 * and copy the output — it tells us whether the problem is:
 *
 *  - the JVM not seeing a TTY at all (System.console() == null)
 *  - konsole's UnixTty declining to attach (isatty returns 0)
 *  - the kernel returning a 0×0 size from TIOCGWINSZ
 *  - missing $TERM / wrong locale
 *
 *   ./demo.sh Diagnostic
 */
public fun main() {
    println("=== JVM ===")
    println("java.version       = ${System.getProperty("java.version")}")
    println("java.vm.name       = ${System.getProperty("java.vm.name")}")
    println("os.name            = ${System.getProperty("os.name")}")
    println("os.version         = ${System.getProperty("os.version")}")
    println("System.console()   = ${System.console()}")
    println()

    println("=== Environment ===")
    for (key in listOf("TERM", "COLORTERM", "LANG", "LC_ALL", "NO_COLOR", "FORCE_COLOR", "TTY", "SSH_TTY")) {
        println("$key = ${System.getenv(key)}")
    }
    println()

    println("=== tools.konsole.core.Terminal.system() ===")
    try {
        Terminal.system().use { t ->
            println("tty impl          = ${t.tty.typeLabel}")
            println("size              = ${t.size.columns} cols x ${t.size.rows} rows")
            println("isatty(stdin)     = ${t.tty.isatty(Stream.Input)}")
            println("isatty(stdout)    = ${t.tty.isatty(Stream.Output)}")
            println("isatty(stderr)    = ${t.tty.isatty(Stream.Error)}")
        }
    } catch (e: Throwable) {
        println("Terminal.system() threw:")
        e.printStackTrace()
    }
}
