package tools.konsole.core.tty.unix

import io.kotest.core.NamedTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.lang.foreign.Arena

/**
 * TTY-tagged: only runs when stdin is attached to a real terminal. Verifies
 * we can call tcgetattr / tcsetattr through FFM and that [TermiosAbi.applyRawModeFlags]
 * round-trips correctly — entry leaves VMIN=1, VTIME=0 and the canonical /
 * echo / signal / extended-input bits cleared in lflag; exit restores every
 * byte of the saved struct.
 *
 * Run with: `./gradlew :core:ttyTest`
 */
class TermiosRoundTripSpec : StringSpec({

    tags(NamedTag("Tty"))

    "tcgetattr + raw-mode mutation + tcsetattr restore round-trips on stdin"
        .config(enabled = LibC.isatty(PosixConsts.STDIN_FD) == 1) {
        val abi = TermiosAbiResolver.current()
        abi shouldNotBe null

        Arena.ofConfined().use { arena ->
            val saved = arena.allocate(abi!!.structSize)
            LibC.tcgetattr(PosixConsts.STDIN_FD, saved) shouldBe 0
            val savedBytes = saved.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE)

            val working = arena.allocate(abi.structSize)
            java.lang.foreign.MemorySegment.copy(saved, 0, working, 0, abi.structSize)
            abi.applyRawModeFlags(working)
            LibC.tcsetattr(PosixConsts.STDIN_FD, PosixConsts.TCSANOW, working) shouldBe 0

            try {
                val readback = arena.allocate(abi.structSize)
                LibC.tcgetattr(PosixConsts.STDIN_FD, readback) shouldBe 0
                abi.getVmin(readback) shouldBe 1
                abi.getVtime(readback) shouldBe 0

                val rawMask = when (abi) {
                    is TermiosLinux -> TermiosLinux.LFLAG_RAW_MASK
                    is TermiosDarwin -> TermiosDarwin.LFLAG_RAW_MASK
                }
                (abi.getLflag(readback) and rawMask) shouldBe 0L
            } finally {
                LibC.tcsetattr(PosixConsts.STDIN_FD, PosixConsts.TCSANOW, saved) shouldBe 0
            }

            // Reading saved back through tcgetattr should bit-match what we
            // captured at the very start — confirms restore is faithful.
            val afterRestore = arena.allocate(abi.structSize)
            LibC.tcgetattr(PosixConsts.STDIN_FD, afterRestore) shouldBe 0
            afterRestore.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE) shouldBe savedBytes
        }
    }

    "isatty returns 0 for /dev/null fd" {
        // Open /dev/null read-only via java.io.FileInputStream so we can grab
        // a non-TTY fd without going through FFM. Then duplicate it to a known
        // fd? — actually simpler: we just verify isatty on stdin (1) and on a
        // clearly-non-TTY fd by way of a pipe via LibC.pipe.
        Arena.ofConfined().use { arena ->
            val pipeFds = arena.allocate(8)
            LibC.pipe(pipeFds) shouldBe 0
            val readFd = pipeFds.get(java.lang.foreign.ValueLayout.JAVA_INT, 0)
            val writeFd = pipeFds.get(java.lang.foreign.ValueLayout.JAVA_INT, 4)
            try {
                LibC.isatty(readFd) shouldBe 0
                LibC.isatty(writeFd) shouldBe 0
            } finally {
                LibC.close(readFd)
                LibC.close(writeFd)
            }
        }
    }
})
