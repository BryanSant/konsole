package tools.konsole.core.tty

import tools.konsole.core.terminal.Size
import tools.konsole.core.tty.unix.LibC
import tools.konsole.core.tty.unix.PosixConsts
import tools.konsole.core.tty.unix.TermiosAbiResolver
import tools.konsole.core.tty.unix.UnixTty
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer

/**
 * Konsole's TTY abstraction. Two implementations select at construction
 * today; a third is planned:
 *
 *  - [tools.konsole.core.tty.unix.UnixTty] — FFM bindings to termios + ioctl
 *    on Linux / macOS.
 *  - `DumbTty` — non-TTY fallback for piped stdin/stdout, tests, headless
 *    environments, and Windows. Raw mode throws.
 *  - `WindowsTty` (planned) — FFM bindings to kernel32 console APIs with
 *    `ENABLE_VIRTUAL_TERMINAL_INPUT` so the byte stream matches Unix.
 *
 * Read sentinels: [EOF] (-1) for end-of-stream including [shutdown];
 * [READ_EXPIRED] (-2) when a timed read hits its deadline.
 *
 * Exposed publicly so power users can reach the low-level stream when the
 * high-level [tools.konsole.core.Terminal] API doesn't cover their use case
 * (raw-byte demos, custom protocol negotiation, etc.).
 */
public interface Tty : AutoCloseable {
    /** Writer for terminal output. UTF-8. */
    public val out: Writer

    /** Best-effort current terminal dimensions. */
    public fun size(): Size

    /** Whether the named standard stream is attached to a TTY. */
    public fun isatty(stream: Stream): Boolean

    /**
     * Read one byte from stdin, blocking until data is available or the Tty
     * is shut down. Returns the byte (0..255) or [EOF] (-1).
     */
    public fun read(): Int

    /**
     * Read one byte from stdin, waiting at most [timeoutMs]. Returns:
     *  - byte (0..255) when data arrives
     *  - [READ_EXPIRED] (-2) when the timeout elapses
     *  - [EOF] (-1) when the Tty is shut down
     */
    public fun read(timeoutMs: Long): Int

    /**
     * Enter raw mode. The returned handle captures the prior termios/console
     * mode; closing it restores. Idempotent only across the matching `close()`.
     */
    public fun enterRawMode(): RawModeHandle

    /** Unblock any pending [read] / [read] calls. Idempotent. */
    public fun shutdown()

    /** Short diagnostic label for the impl, e.g. `"unix-linux"`, `"dumb"`. */
    public val typeLabel: String

    public companion object {
        public const val EOF: Int = -1
        public const val READ_EXPIRED: Int = -2
    }
}

/** Identifies one of the three standard streams. */
public enum class Stream { Input, Output, Error }

/**
 * Returned by [Tty.enterRawMode]. Implements [AutoCloseable] so `use { }`
 * restores the saved termios on any exit path.
 */
public interface RawModeHandle : AutoCloseable {
    /** Restore the saved termios / console mode. Safe to call more than once. */
    override fun close()
}

/**
 * Construct [Tty] instances. Detects the running OS and stream-by-stream
 * TTY-ness; falls back to a dumb impl when the process isn't attached to a
 * console (piped input, redirected output, CI runners).
 */
public object TtyFactory {
    /**
     * Open the system TTY. Falls back to [dumb] when stdin or stdout aren't
     * character devices, or when the host OS isn't supported yet.
     */
    public fun system(): Tty {
        val abi = TermiosAbiResolver.current()
        if (abi != null &&
            LibC.isatty(PosixConsts.STDIN_FD) == 1 &&
            LibC.isatty(PosixConsts.STDOUT_FD) == 1
        ) {
            return runCatching { UnixTty(abi) }.getOrElse { dumb(System.`in`, System.out) }
        }
        return dumb(System.`in`, System.out)
    }

    /** Non-TTY fallback over arbitrary streams. */
    public fun dumb(input: InputStream, output: OutputStream): Tty = DumbTty(input, output)
}
