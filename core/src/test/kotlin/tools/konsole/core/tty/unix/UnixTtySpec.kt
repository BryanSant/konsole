package tools.konsole.core.tty.unix

import io.kotest.core.NamedTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import tools.konsole.core.tty.Stream
import tools.konsole.core.tty.Tty

/**
 * TTY-tagged: requires stdin and stdout to be attached to a real terminal.
 * Run with: `./gradlew :core:ttyTest`.
 */
class UnixTtySpec : StringSpec({

    tags(NamedTag("Tty"))

    val hasTty = LibC.isatty(PosixConsts.STDIN_FD) == 1 &&
        LibC.isatty(PosixConsts.STDOUT_FD) == 1

    "size() returns a positive non-fallback dimension".config(enabled = hasTty) {
        val abi = TermiosAbiResolver.current() shouldNotBe null
        UnixTty(abi!!).use { tty ->
            val s = tty.size()
            (s.columns > 0) shouldBe true
            (s.rows > 0) shouldBe true
        }
    }

    "isatty matches LibC.isatty on each standard stream".config(enabled = hasTty) {
        val abi = TermiosAbiResolver.current()!!
        UnixTty(abi).use { tty ->
            tty.isatty(Stream.Input) shouldBe true
            tty.isatty(Stream.Output) shouldBe true
            // stderr may or may not be a TTY under the test runner. Just
            // verify the call doesn't throw and returns a Boolean consistent
            // with libc.
            tty.isatty(Stream.Error) shouldBe (LibC.isatty(PosixConsts.STDERR_FD) == 1)
        }
    }

    "enterRawMode round-trips: VMIN=1, VTIME=0, restore is byte-faithful"
        .config(enabled = hasTty) {
        val abi = TermiosAbiResolver.current()!!
        UnixTty(abi).use { tty ->
            // Capture saved state by hand so we can compare bytes after restore.
            java.lang.foreign.Arena.ofConfined().use { arena ->
                val before = arena.allocate(abi.structSize)
                LibC.tcgetattr(PosixConsts.STDIN_FD, before) shouldBe 0
                val beforeBytes = before.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE)

                val handle = tty.enterRawMode()

                val mid = arena.allocate(abi.structSize)
                LibC.tcgetattr(PosixConsts.STDIN_FD, mid) shouldBe 0
                abi.getVmin(mid) shouldBe 1
                abi.getVtime(mid) shouldBe 0

                handle.close()
                // Double-close is allowed and is a no-op.
                handle.close()

                val after = arena.allocate(abi.structSize)
                LibC.tcgetattr(PosixConsts.STDIN_FD, after) shouldBe 0
                after.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE) shouldBe beforeBytes
            }
        }
    }

    "shutdown unblocks a parked read".config(enabled = hasTty) {
        val abi = TermiosAbiResolver.current()!!
        UnixTty(abi).use { tty ->
            val result = java.util.concurrent.CompletableFuture<Int>()
            val reader = Thread { result.complete(tty.read(5_000L)) }
                .apply { isDaemon = true; start() }
            Thread.sleep(50)
            tty.shutdown()
            reader.join(2_000)
            result.get() shouldBe Tty.EOF
        }
    }

    "read after shutdown returns EOF immediately".config(enabled = hasTty) {
        val abi = TermiosAbiResolver.current()!!
        UnixTty(abi).use { tty ->
            tty.shutdown()
            tty.read() shouldBe Tty.EOF
            tty.read(100L) shouldBe Tty.EOF
        }
    }

    "read(timeoutMs) with no input returns READ_EXPIRED".config(enabled = hasTty) {
        val abi = TermiosAbiResolver.current()!!
        UnixTty(abi).use { tty ->
            // 20ms is enough to assert the path; if stdin happens to have
            // queued data this test will be flaky, but under the ttyTest task
            // we're attached to a parent terminal and no one is typing.
            tty.read(20L) shouldBe Tty.READ_EXPIRED
        }
    }

    "typeLabel reflects the resolved ABI".config(enabled = hasTty) {
        val abi = TermiosAbiResolver.current()!!
        UnixTty(abi).use { tty ->
            val expected = when (abi) {
                is TermiosLinux -> "unix-linux"
                is TermiosDarwin -> "unix-darwin"
            }
            tty.typeLabel shouldBe expected
        }
    }
})
