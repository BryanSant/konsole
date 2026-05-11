package tools.konsole.core

import tools.konsole.core.Command
import tools.konsole.core.Ansi.OSC
import tools.konsole.core.Ansi.ST
import java.util.Base64

/**
 * The clipboard a copy operation should target. Some platforms expose multiple
 * clipboards (notably X11/Wayland with primary + clipboard); see the
 * [Freedesktop clipboard spec](https://specifications.freedesktop.org/clipboard-spec/latest/).
 */
public sealed interface ClipboardDestination {
    /** The OSC 52 `Pc` letter for this destination. */
    public val pc: Char

    /** Default clipboard (Ctrl+C / Ctrl+V on most platforms). */
    public data object Clipboard : ClipboardDestination {
        override val pc: Char get() = 'c'
    }

    /** X11/Wayland selection clipboard (middle-click paste). */
    public data object Primary : ClipboardDestination {
        override val pc: Char get() = 'p'
    }

    /** Any other Pc letter from the OSC 52 spec. */
    public data class Other(override val pc: Char) : ClipboardDestination
}

/** Maximum payload size most terminals tolerate for OSC 52 (in raw bytes, pre-base64). */
public const val DEFAULT_CLIPBOARD_MAX_BYTES: Int = 100_000

/** Thrown when [CopyToClipboard.maxBytes] would be exceeded. */
public class ClipboardTooLargeException(
    public val payloadBytes: Int,
    public val maxBytes: Int,
) : IllegalArgumentException("Clipboard payload of $payloadBytes bytes exceeds the maximum of $maxBytes bytes.")

/**
 * Copy [content] to the host terminal's clipboard via OSC 52.
 *
 * Many terminals refuse OSC 52 by default for security reasons; the user must
 * opt in via terminal settings (xterm, kitty, foot, alacritty, wezterm).
 *
 * @param destinations the clipboards to target. Defaults to the primary clipboard only;
 *   pass `setOf(Clipboard, Primary)` on Linux for both.
 * @param maxBytes guard against accidentally constructing a huge escape sequence —
 *   throws [ClipboardTooLargeException] if [content] exceeds this.
 */
public data class CopyToClipboard<T>(
    val content: T,
    val destinations: Set<ClipboardDestination> = setOf(ClipboardDestination.Clipboard),
    val maxBytes: Int = DEFAULT_CLIPBOARD_MAX_BYTES,
) : Command {
    init {
        require(destinations.isNotEmpty()) { "destinations must not be empty" }
    }

    override fun writeAnsi(out: Appendable) {
        val raw = content.toString().toByteArray(Charsets.UTF_8)
        if (raw.size > maxBytes) throw ClipboardTooLargeException(raw.size, maxBytes)
        val pc = destinations.joinToString(separator = "") { it.pc.toString() }
        val pd = Base64.getEncoder().encodeToString(raw)
        out.append(OSC).append("52;").append(pc).append(';').append(pd).append(ST)
    }
}
