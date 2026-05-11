package tools.konsole.core

/**
 * ANSI control-sequence string constants used to assemble escape sequences.
 *
 * All escape-sequence emission throughout konsole references these constants —
 * never a literal ESC byte appears in source. Most commands emit a CSI sequence
 * (`ESC [` … final byte). OSC sequences (`ESC ]` … BEL or ST) carry payloads
 * such as links (OSC 8), notifications (OSC 9), and clipboard data (OSC 52).
 */
public object Ansi {
    /** ESC (`\u001B`). */
    public const val ESC: String = "\u001B"

    /** Control Sequence Introducer (`ESC [`). */
    public const val CSI: String = "\u001B["

    /** Single Shift 3 (`ESC O`) — function-key prefix used by some terminals. */
    public const val SS3: String = "\u001BO"

    /** Operating System Command introducer (`ESC ]`). */
    public const val OSC: String = "\u001B]"

    /** Device Control String introducer (`ESC P`). */
    public const val DCS: String = "\u001BP"

    /** String Terminator (`ESC \`) — closes OSC, DCS, and APC sequences. */
    public const val ST: String = "\u001B\\"

    /** Bell (`\u0007`) — alternative OSC terminator. */
    public const val BEL: String = "\u0007"
}
