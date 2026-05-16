package tools.konsole.core.tty

import tools.konsole.core.terminal.Size
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.Charsets.UTF_8

/**
 * Non-TTY [Tty]: reads from an arbitrary [InputStream] and writes to an
 * arbitrary [OutputStream]. Used when the process isn't attached to a console
 * (piped stdin, redirected stdout, headless CI), and in unit tests.
 *
 * Raw mode is unsupported: [enterRawMode] throws. [size] returns the
 * conventional 80x24 fallback so callers that ask without checking [isatty]
 * still get a sensible answer.
 */
internal class DumbTty(
    private val input: InputStream,
    output: OutputStream,
) : Tty {

    override val out: Writer = BufferedWriter(OutputStreamWriter(output, UTF_8))

    private val stopped = AtomicBoolean(false)

    override fun size(): Size = Size(80, 24)

    override fun isatty(stream: Stream): Boolean = false

    override fun read(): Int {
        if (stopped.get()) return Tty.EOF
        return try {
            input.read()
        } catch (_: java.io.InterruptedIOException) {
            Tty.EOF
        }
    }

    override fun read(timeoutMs: Long): Int {
        if (stopped.get()) return Tty.EOF
        // Best-effort: if data is immediately available, return it; otherwise
        // wait up to timeoutMs in small slices since arbitrary InputStreams
        // don't expose a real timeout.
        val deadline = System.nanoTime() + timeoutMs * 1_000_000L
        while (!stopped.get()) {
            if (input.available() > 0) return read()
            if (System.nanoTime() >= deadline) return Tty.READ_EXPIRED
            Thread.sleep(1)
        }
        return Tty.EOF
    }

    override fun enterRawMode(): RawModeHandle =
        throw UnsupportedOperationException("DumbTty does not support raw mode (stdin is not a TTY)")

    override fun shutdown() {
        stopped.set(true)
    }

    override fun close() {
        shutdown()
        try { out.flush() } catch (_: Throwable) { /* ignore */ }
    }

    override val typeLabel: String = "dumb"
}
