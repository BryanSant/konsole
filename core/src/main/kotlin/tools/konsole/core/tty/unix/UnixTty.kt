package tools.konsole.core.tty.unix

import tools.konsole.core.terminal.Size
import tools.konsole.core.tty.RawModeHandle
import tools.konsole.core.tty.Stream
import tools.konsole.core.tty.Tty
import java.io.BufferedWriter
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.Charsets.UTF_8

/**
 * [Tty] backed by direct libc calls through FFM. Active on Linux and macOS
 * when stdin/stdout are real character devices.
 *
 * Input goes through `poll(2)` so [read] supports both blocking and
 * time-bounded waits without spinning. A self-pipe is added to the poll set
 * so [shutdown] can wake a thread parked inside `poll`.
 *
 * Output goes through `FileOutputStream(FileDescriptor.out)`. Same underlying
 * `write(2)` as `LibC.write`, but plays nicely with Java's BufferedWriter and
 * charset machinery — and matches how stdout is treated everywhere else.
 */
internal class UnixTty(
    private val abi: TermiosAbi,
) : Tty {

    private val arena: Arena = Arena.ofShared()

    private val wakeReadFd: Int
    private val wakeWriteFd: Int

    private val pollFds: MemorySegment = arena.allocate(PosixConsts.SIZEOF_POLLFD * 2)
    private val readBuf: MemorySegment = arena.allocate(1)
    private val drainBuf: MemorySegment = arena.allocate(64)
    private val winsizeBuf: MemorySegment = arena.allocate(PosixConsts.SIZEOF_WINSIZE)
    private val wakeOneByte: MemorySegment = arena.allocate(1).also {
        it.set(ValueLayout.JAVA_BYTE, 0, 0)
    }

    private val stopped = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    override val out: Writer = BufferedWriter(
        OutputStreamWriter(FileOutputStream(FileDescriptor.out), UTF_8),
    )

    override val typeLabel: String = "unix-" + when (abi) {
        is TermiosLinux -> "linux"
        is TermiosDarwin -> "darwin"
    }

    init {
        Arena.ofConfined().use { confined ->
            val seg = confined.allocate(8)
            val rc = LibC.pipe(seg)
            check(rc == 0) { "pipe(2) failed setting up TTY wake pipe" }
            wakeReadFd = seg.get(ValueLayout.JAVA_INT, 0)
            wakeWriteFd = seg.get(ValueLayout.JAVA_INT, 4)
        }

        // pollfd[0] = stdin
        pollFds.set(ValueLayout.JAVA_INT, PosixConsts.POLLFD_FD_OFFSET, PosixConsts.STDIN_FD)
        pollFds.set(ValueLayout.JAVA_SHORT, PosixConsts.POLLFD_EVENTS_OFFSET, PosixConsts.POLLIN)
        // pollfd[1] = wake-read end of self-pipe
        pollFds.set(
            ValueLayout.JAVA_INT,
            PosixConsts.SIZEOF_POLLFD + PosixConsts.POLLFD_FD_OFFSET,
            wakeReadFd,
        )
        pollFds.set(
            ValueLayout.JAVA_SHORT,
            PosixConsts.SIZEOF_POLLFD + PosixConsts.POLLFD_EVENTS_OFFSET,
            PosixConsts.POLLIN,
        )
    }

    override fun size(): Size {
        if (LibC.ioctl(PosixConsts.STDOUT_FD, abi.tiocgwinsz, winsizeBuf) != 0) {
            return Size(columns = 80, rows = 24)
        }
        val rows = winsizeBuf.get(ValueLayout.JAVA_SHORT, PosixConsts.WINSIZE_ROW_OFFSET).toInt() and 0xFFFF
        val cols = winsizeBuf.get(ValueLayout.JAVA_SHORT, PosixConsts.WINSIZE_COL_OFFSET).toInt() and 0xFFFF
        return Size(
            columns = if (cols > 0) cols else 80,
            rows = if (rows > 0) rows else 24,
        )
    }

    override fun isatty(stream: Stream): Boolean {
        val fd = when (stream) {
            Stream.Input -> PosixConsts.STDIN_FD
            Stream.Output -> PosixConsts.STDOUT_FD
            Stream.Error -> PosixConsts.STDERR_FD
        }
        return LibC.isatty(fd) == 1
    }

    override fun read(): Int = readInternal(-1)

    override fun read(timeoutMs: Long): Int {
        require(timeoutMs >= 0) { "timeoutMs must be non-negative" }
        return readInternal(timeoutMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    private fun readInternal(timeoutMs: Int): Int {
        if (stopped.get()) return Tty.EOF

        // Clear revents on both pollfds — poll() writes them, but we want to
        // be sure we're not reading stale values on a retry path.
        pollFds.set(ValueLayout.JAVA_SHORT, PosixConsts.POLLFD_REVENTS_OFFSET, 0)
        pollFds.set(
            ValueLayout.JAVA_SHORT,
            PosixConsts.SIZEOF_POLLFD + PosixConsts.POLLFD_REVENTS_OFFSET,
            0,
        )

        val n = LibC.poll(pollFds, 2, timeoutMs)
        if (n == 0) return Tty.READ_EXPIRED
        if (n < 0) {
            // EINTR or similar transient failure. Tell the caller nothing
            // happened; they'll loop and retry. If we were shut down in the
            // meantime, the next call returns EOF immediately.
            return if (stopped.get()) Tty.EOF else Tty.READ_EXPIRED
        }

        val wakeRevents = pollFds
            .get(ValueLayout.JAVA_SHORT, PosixConsts.SIZEOF_POLLFD + PosixConsts.POLLFD_REVENTS_OFFSET)
            .toInt() and 0xFFFF
        if (wakeRevents != 0) {
            // Drain the pipe so subsequent polls don't return immediately.
            LibC.read(wakeReadFd, drainBuf, drainBuf.byteSize())
            return Tty.EOF
        }

        val stdinRevents = pollFds
            .get(ValueLayout.JAVA_SHORT, PosixConsts.POLLFD_REVENTS_OFFSET)
            .toInt() and 0xFFFF
        if (stdinRevents and PosixConsts.POLLIN.toInt() != 0) {
            val got = LibC.read(PosixConsts.STDIN_FD, readBuf, 1)
            if (got <= 0) return Tty.EOF
            return readBuf.get(ValueLayout.JAVA_BYTE, 0).toInt() and 0xFF
        }
        // POLLHUP / POLLERR on stdin — treat as EOF.
        return Tty.EOF
    }

    override fun enterRawMode(): RawModeHandle {
        // Saved-termios lives on its own shared arena so the handle's restore
        // is independent of the Tty's lifetime — a caller can hold a handle
        // and close the tty in either order. Shared (not confined) so close
        // can run from any thread.
        val handleArena = Arena.ofShared()
        try {
            val saved = handleArena.allocate(abi.structSize)
            check(LibC.tcgetattr(PosixConsts.STDIN_FD, saved) == 0) {
                "tcgetattr failed; stdin is not a TTY"
            }
            Arena.ofConfined().use { tmp ->
                val working = tmp.allocate(abi.structSize)
                MemorySegment.copy(saved, 0, working, 0, abi.structSize)
                abi.applyRawModeFlags(working)
                check(LibC.tcsetattr(PosixConsts.STDIN_FD, PosixConsts.TCSANOW, working) == 0) {
                    "tcsetattr failed entering raw mode"
                }
            }
            val restored = AtomicBoolean(false)
            return object : RawModeHandle {
                override fun close() {
                    if (restored.compareAndSet(false, true)) {
                        try {
                            LibC.tcsetattr(PosixConsts.STDIN_FD, PosixConsts.TCSANOW, saved)
                        } finally {
                            handleArena.close()
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            handleArena.close()
            throw t
        }
    }

    override fun shutdown() {
        if (stopped.compareAndSet(false, true)) {
            // One byte is enough to wake poll(); errors are ignored — at worst
            // a blocked reader will time out on its own.
            LibC.write(wakeWriteFd, wakeOneByte, 1)
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        shutdown()
        try { out.flush() } catch (_: Throwable) { /* ignore */ }
        LibC.close(wakeReadFd)
        LibC.close(wakeWriteFd)
        arena.close()
    }
}
