package tools.konsole.examples

import org.jline.terminal.TerminalBuilder
import org.jline.terminal.spi.SystemStream
import org.jline.terminal.spi.TerminalProvider

/**
 * Dump everything we know about the JVM/terminal connection. When a demo
 * looks broken (renders into 1 column, reports size 0×0, etc.), run this
 * and copy the output — it tells us whether the problem is:
 *
 *  - the JVM not seeing a TTY at all (System.console() == null)
 *  - JLine FFM declining to attach (isSystemStream == false)
 *  - JLine attaching but the kernel returning a 0×0 size
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

    println("=== JLine FFM provider ===")
    try {
        val cls = Class.forName("org.jline.terminal.impl.ffm.FfmTerminalProvider")
        val provider = cls.getDeclaredConstructor().newInstance() as TerminalProvider
        println("provider loaded: $provider")
        for (s in listOf(SystemStream.Input, SystemStream.Output, SystemStream.Error)) {
            val sys = try { provider.isSystemStream(s) } catch (e: Throwable) { "threw: $e" }
            val width = try { provider.systemStreamWidth(s) } catch (e: Throwable) { "threw: $e" }
            val name = try { provider.systemStreamName(s) } catch (e: Throwable) { "threw: $e" }
            println("  $s isSystem=$sys width=$width name=$name")
        }
    } catch (e: Throwable) {
        println("FFM provider failed to load:")
        e.printStackTrace()
    }
    println()

    println("=== TerminalBuilder.system(true).build() ===")
    try {
        TerminalBuilder.builder().system(true).build().use { t ->
            println("class    = ${t.javaClass.name}")
            println("type     = ${t.type}")
            println("size     = ${t.size}")
            println("encoding = ${t.encoding()}")
        }
    } catch (e: Throwable) {
        println("build() threw:")
        e.printStackTrace()
    }
    println()

    println("=== TerminalBuilder.dumb(false).build() (forces FFM, throws on fallback) ===")
    try {
        TerminalBuilder.builder().system(true).dumb(false).build().use { t ->
            println("class    = ${t.javaClass.name}")
            println("type     = ${t.type}")
            println("size     = ${t.size}")
        }
    } catch (e: Throwable) {
        println("build() threw (this reveals the real FFM failure cause):")
        e.printStackTrace()
    }
}
