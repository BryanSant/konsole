package tools.konsole.core.tty.unix

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Per-OS layout and constants for `struct termios`. Linux and macOS use
 * different `tcflag_t` widths (4 vs 8 bytes), different `NCCS`, and different
 * mask values for nearly every flag — so each OS implements this interface
 * with its own offsets and constants. [UnixTty] picks one at construction
 * based on `os.name`.
 *
 * The contract is small on purpose: read/write the four `tcflag_t` fields,
 * read/write the two control characters we care about (`VMIN`, `VTIME`), and
 * apply [applyRawModeFlags] — which masks off canonical/echo/signal bits and
 * sets `VMIN=1, VTIME=0`. We never touch `c_ispeed`/`c_ospeed`; tcgetattr
 * populates them and tcsetattr round-trips them unchanged.
 */
internal sealed interface TermiosAbi {

    /** sizeof(struct termios) on this OS. */
    val structSize: Long

    /** sizeof(struct winsize) ioctl request constant for this OS. */
    val tiocgwinsz: Long

    /**
     * Apply our raw-mode flag mask in place. Caller has already called
     * `tcgetattr` to populate [seg]; this mutates the four flag fields and
     * sets `c_cc[VMIN]=1, c_cc[VTIME]=0`. After this returns, pass [seg] to
     * `tcsetattr(fd, TCSANOW, seg)` to apply.
     */
    fun applyRawModeFlags(seg: MemorySegment)

    /** Read `c_cc[VMIN]`. Mainly used by tests to verify a round-trip. */
    fun getVmin(seg: MemorySegment): Int

    /** Read `c_cc[VTIME]`. */
    fun getVtime(seg: MemorySegment): Int

    /** Read `c_lflag`. Tests verify masked bits are off after raw-mode entry. */
    fun getLflag(seg: MemorySegment): Long
}

/**
 * Linux glibc `struct termios` (60 bytes, NCCS=32, 4-byte tcflag_t).
 *
 *   off  size  field
 *     0    4   tcflag_t c_iflag
 *     4    4   tcflag_t c_oflag
 *     8    4   tcflag_t c_cflag
 *    12    4   tcflag_t c_lflag
 *    16    1   cc_t     c_line
 *    17   32   cc_t     c_cc[NCCS]
 *    49    3   <padding>
 *    52    4   speed_t  c_ispeed
 *    56    4   speed_t  c_ospeed
 */
internal object TermiosLinux : TermiosAbi {
    override val structSize: Long = 60
    override val tiocgwinsz: Long = 0x5413L

    private const val IFLAG_OFFSET: Long = 0
    private const val OFLAG_OFFSET: Long = 4
    private const val CFLAG_OFFSET: Long = 8
    private const val LFLAG_OFFSET: Long = 12
    private const val CC_BASE_OFFSET: Long = 17
    private const val VTIME_INDEX: Int = 5
    private const val VMIN_INDEX: Int = 6

    // From <asm-generic/termbits.h>.
    private const val IXON: Int = 0x0400
    private const val ICRNL: Int = 0x0100
    private const val BRKINT: Int = 0x0002
    private const val INPCK: Int = 0x0010
    private const val ISTRIP: Int = 0x0020
    private const val OPOST: Int = 0x0001
    private const val CSIZE: Int = 0x0030
    private const val CS8: Int = 0x0030
    private const val PARENB: Int = 0x0100
    private const val ECHO: Int = 0x0008
    private const val ICANON: Int = 0x0002
    private const val ISIG: Int = 0x0001
    private const val IEXTEN: Int = 0x8000

    override fun applyRawModeFlags(seg: MemorySegment) {
        // iflag: drop input translation and flow control.
        var iflag = seg.get(ValueLayout.JAVA_INT, IFLAG_OFFSET).toLong() and 0xFFFF_FFFFL
        iflag = iflag and (IXON or ICRNL or BRKINT or INPCK or ISTRIP).inv().toLong().and(0xFFFF_FFFFL)
        seg.set(ValueLayout.JAVA_INT, IFLAG_OFFSET, iflag.toInt())

        // oflag: disable output post-processing (no \n -> \r\n).
        var oflag = seg.get(ValueLayout.JAVA_INT, OFLAG_OFFSET).toLong() and 0xFFFF_FFFFL
        oflag = oflag and OPOST.inv().toLong().and(0xFFFF_FFFFL)
        seg.set(ValueLayout.JAVA_INT, OFLAG_OFFSET, oflag.toInt())

        // cflag: force 8-bit chars, no parity.
        var cflag = seg.get(ValueLayout.JAVA_INT, CFLAG_OFFSET).toLong() and 0xFFFF_FFFFL
        cflag = cflag and (CSIZE or PARENB).inv().toLong().and(0xFFFF_FFFFL)
        cflag = cflag or CS8.toLong()
        seg.set(ValueLayout.JAVA_INT, CFLAG_OFFSET, cflag.toInt())

        // lflag: kill echo, canonical mode, signal generation, extended input.
        var lflag = seg.get(ValueLayout.JAVA_INT, LFLAG_OFFSET).toLong() and 0xFFFF_FFFFL
        lflag = lflag and (ECHO or ICANON or ISIG or IEXTEN).inv().toLong().and(0xFFFF_FFFFL)
        seg.set(ValueLayout.JAVA_INT, LFLAG_OFFSET, lflag.toInt())

        // VMIN=1, VTIME=0: read() blocks until at least one byte arrives.
        seg.set(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VMIN_INDEX, 1)
        seg.set(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VTIME_INDEX, 0)
    }

    override fun getVmin(seg: MemorySegment): Int =
        seg.get(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VMIN_INDEX).toInt() and 0xFF

    override fun getVtime(seg: MemorySegment): Int =
        seg.get(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VTIME_INDEX).toInt() and 0xFF

    override fun getLflag(seg: MemorySegment): Long =
        seg.get(ValueLayout.JAVA_INT, LFLAG_OFFSET).toLong() and 0xFFFF_FFFFL

    // Exposed for tests so they can assert which bits we cleared.
    val LFLAG_RAW_MASK: Long = (ECHO or ICANON or ISIG or IEXTEN).toLong()
}

/**
 * macOS / Darwin `struct termios` (72 bytes, NCCS=20, 8-byte tcflag_t).
 *
 *   off  size  field
 *     0    8   tcflag_t c_iflag      (unsigned long)
 *     8    8   tcflag_t c_oflag
 *    16    8   tcflag_t c_cflag
 *    24    8   tcflag_t c_lflag
 *    32   20   cc_t     c_cc[NCCS]
 *    52    4   <padding to 8-byte align speed_t>
 *    56    8   speed_t  c_ispeed       (unsigned long)
 *    64    8   speed_t  c_ospeed
 */
internal object TermiosDarwin : TermiosAbi {
    override val structSize: Long = 72
    override val tiocgwinsz: Long = 0x40087468L

    private const val IFLAG_OFFSET: Long = 0
    private const val OFLAG_OFFSET: Long = 8
    private const val CFLAG_OFFSET: Long = 16
    private const val LFLAG_OFFSET: Long = 24
    private const val CC_BASE_OFFSET: Long = 32
    private const val VMIN_INDEX: Int = 16
    private const val VTIME_INDEX: Int = 17

    // From <sys/termios.h> on macOS.
    private const val IXON: Long = 0x00000200
    private const val ICRNL: Long = 0x00000100
    private const val BRKINT: Long = 0x00000002
    private const val INPCK: Long = 0x00000010
    private const val ISTRIP: Long = 0x00000020
    private const val OPOST: Long = 0x00000001
    private const val CSIZE: Long = 0x00000300
    private const val CS8: Long = 0x00000300
    private const val PARENB: Long = 0x00001000
    private const val ECHO: Long = 0x00000008
    private const val ICANON: Long = 0x00000100
    private const val ISIG: Long = 0x00000080
    private const val IEXTEN: Long = 0x00000400

    override fun applyRawModeFlags(seg: MemorySegment) {
        var iflag = seg.get(ValueLayout.JAVA_LONG, IFLAG_OFFSET)
        iflag = iflag and (IXON or ICRNL or BRKINT or INPCK or ISTRIP).inv()
        seg.set(ValueLayout.JAVA_LONG, IFLAG_OFFSET, iflag)

        var oflag = seg.get(ValueLayout.JAVA_LONG, OFLAG_OFFSET)
        oflag = oflag and OPOST.inv()
        seg.set(ValueLayout.JAVA_LONG, OFLAG_OFFSET, oflag)

        var cflag = seg.get(ValueLayout.JAVA_LONG, CFLAG_OFFSET)
        cflag = cflag and (CSIZE or PARENB).inv()
        cflag = cflag or CS8
        seg.set(ValueLayout.JAVA_LONG, CFLAG_OFFSET, cflag)

        var lflag = seg.get(ValueLayout.JAVA_LONG, LFLAG_OFFSET)
        lflag = lflag and (ECHO or ICANON or ISIG or IEXTEN).inv()
        seg.set(ValueLayout.JAVA_LONG, LFLAG_OFFSET, lflag)

        seg.set(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VMIN_INDEX, 1)
        seg.set(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VTIME_INDEX, 0)
    }

    override fun getVmin(seg: MemorySegment): Int =
        seg.get(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VMIN_INDEX).toInt() and 0xFF

    override fun getVtime(seg: MemorySegment): Int =
        seg.get(ValueLayout.JAVA_BYTE, CC_BASE_OFFSET + VTIME_INDEX).toInt() and 0xFF

    override fun getLflag(seg: MemorySegment): Long =
        seg.get(ValueLayout.JAVA_LONG, LFLAG_OFFSET)

    val LFLAG_RAW_MASK: Long = ECHO or ICANON or ISIG or IEXTEN
}

internal object TermiosAbiResolver {
    /** Pick the right [TermiosAbi] for the running OS, or null if unsupported. */
    fun current(): TermiosAbi? {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            "linux" in os -> TermiosLinux
            "mac" in os || "darwin" in os -> TermiosDarwin
            else -> null
        }
    }
}
