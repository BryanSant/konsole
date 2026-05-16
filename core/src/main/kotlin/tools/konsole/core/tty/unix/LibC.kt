package tools.konsole.core.tty.unix

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

/**
 * Thin FFM wrapper around the subset of libc we need for raw-mode TTY I/O.
 *
 * Functions are resolved once at class-load via the platform's default symbol
 * lookup (libSystem on macOS, libc.so.6 on Linux). Method handles are reused
 * across calls. Memory is allocated by the caller — for short-lived buffers
 * use [Arena.ofConfined], for buffers held across a Tty's lifetime pass an
 * [Arena.ofShared] from the owning impl.
 *
 * Convention: every wrapper returns the raw `int` / `long` from libc unchanged
 * so callers can examine `errno` paths if needed. We never throw from this
 * layer — error handling lives in [UnixTty].
 */
internal object LibC {
    private val LINKER: Linker = Linker.nativeLinker()
    private val LOOKUP = LINKER.defaultLookup()

    private fun handle(name: String, descriptor: FunctionDescriptor, vararg options: Linker.Option) =
        LINKER.downcallHandle(
            LOOKUP.find(name).orElseThrow { UnsatisfiedLinkError("libc: $name not found") },
            descriptor,
            *options,
        )

    private val isattyHandle = handle("isatty", FunctionDescriptor.of(JAVA_INT, JAVA_INT))
    private val tcgetattrHandle = handle("tcgetattr", FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS))
    private val tcsetattrHandle = handle("tcsetattr", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS))

    // ioctl is variadic; the third arg's type depends on the request. For
    // TIOCGWINSZ it's a `struct winsize*` (we pass it as ADDRESS).
    private val ioctlHandle = handle(
        "ioctl",
        FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS),
        Linker.Option.firstVariadicArg(2),
    )

    // read returns ssize_t — JAVA_LONG is correct on every 64-bit Unix we
    // care about (LP64). On the off chance the JVM ever targets an ILP32
    // Unix, this would need a per-platform override.
    private val readHandle = handle("read", FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG))
    private val writeHandle = handle("write", FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG))

    // poll: int poll(struct pollfd *fds, nfds_t nfds, int timeout)
    // nfds_t is unsigned long on Linux, unsigned int on Darwin — they fit in
    // JAVA_LONG either way (we never pass values beyond a handful).
    private val pollHandle = handle("poll", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT))

    private val pipeHandle = handle("pipe", FunctionDescriptor.of(JAVA_INT, ADDRESS))
    private val closeHandle = handle("close", FunctionDescriptor.of(JAVA_INT, JAVA_INT))

    // ---- Kotlin-facing wrappers ----

    fun isatty(fd: Int): Int = isattyHandle.invokeExact(fd) as Int

    fun tcgetattr(fd: Int, termios: MemorySegment): Int =
        tcgetattrHandle.invokeExact(fd, termios) as Int

    fun tcsetattr(fd: Int, optionalActions: Int, termios: MemorySegment): Int =
        tcsetattrHandle.invokeExact(fd, optionalActions, termios) as Int

    fun ioctl(fd: Int, request: Long, arg: MemorySegment): Int =
        ioctlHandle.invokeExact(fd, request, arg) as Int

    fun read(fd: Int, buf: MemorySegment, count: Long): Long =
        readHandle.invokeExact(fd, buf, count) as Long

    fun write(fd: Int, buf: MemorySegment, count: Long): Long =
        writeHandle.invokeExact(fd, buf, count) as Long

    fun poll(fds: MemorySegment, nfds: Long, timeoutMs: Int): Int =
        pollHandle.invokeExact(fds, nfds, timeoutMs) as Int

    fun pipe(twoInts: MemorySegment): Int = pipeHandle.invokeExact(twoInts) as Int

    fun close(fd: Int): Int = closeHandle.invokeExact(fd) as Int
}

/**
 * POSIX-stable constants we need without consulting a per-OS header.
 */
internal object PosixConsts {
    /** Apply termios change immediately. */
    const val TCSANOW: Int = 0

    /** Standard file descriptors. */
    const val STDIN_FD: Int = 0
    const val STDOUT_FD: Int = 1
    const val STDERR_FD: Int = 2

    /** poll(2) revents flag — data available to read. */
    const val POLLIN: Short = 0x0001

    /** sizeof(struct pollfd) — { int fd; short events; short revents; } = 8. */
    const val SIZEOF_POLLFD: Long = 8
    const val POLLFD_FD_OFFSET: Long = 0
    const val POLLFD_EVENTS_OFFSET: Long = 4
    const val POLLFD_REVENTS_OFFSET: Long = 6

    /** struct winsize layout: { unsigned short ws_row, ws_col, ws_xpixel, ws_ypixel } = 8 bytes. */
    const val SIZEOF_WINSIZE: Long = 8
    const val WINSIZE_ROW_OFFSET: Long = 0
    const val WINSIZE_COL_OFFSET: Long = 2

    fun layoutValueShort(): ValueLayout = ValueLayout.JAVA_SHORT
    fun layoutValueByte(): ValueLayout = JAVA_BYTE
}
