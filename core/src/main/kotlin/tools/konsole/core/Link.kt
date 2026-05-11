package tools.konsole.core

import tools.konsole.core.Ansi.OSC
import tools.konsole.core.Ansi.ST
import tools.konsole.core.style.StyledContent

/**
 * Begin an OSC 8 clickable link. Text printed between this command and
 * [EndLink] becomes a link in supported terminals (iTerm2, Kitty, WezTerm,
 * GNOME Terminal, foot, etc.).
 *
 * @param uri target URI (e.g. `"https://example.com"`)
 * @param params optional `key=value` params recognized by the terminal — most
 *   commonly `id=...` to coalesce links spanning multiple lines.
 */
public data class StartLink(
    val uri: String,
    val params: Map<String, String> = emptyMap(),
) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("8;")
        var first = true
        for ((k, v) in params) {
            if (!first) out.append(':')
            out.append(k).append('=').append(v)
            first = false
        }
        out.append(';').append(uri).append(ST)
    }
}

/** End the most recently opened OSC 8 link. */
public data object EndLink : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("8;;").append(ST)
    }
}

/**
 * Content wrapped in an OSC 8 clickable link.
 *
 * Obtain via [String.link] or [StyledContent.link]:
 * ```
 * println("Kotlin".link("https://kotlinlang.org"))
 * println("Kotlin".bold().cyan().link("https://kotlinlang.org"))
 * ```
 *
 * [toString] emits the full `OSC 8 … ST  content  OSC 8 ;; ST` sequence,
 * so instances can be passed directly to [print][tools.konsole.core.TerminalScope.print]
 * or used inside string templates.
 */
public data class LinkContent<out D>(
    val uri: String,
    val content: D,
    val params: Map<String, String> = emptyMap(),
) {
    override fun toString(): String = buildString {
        StartLink(uri, params).writeAnsi(this)
        append(content.toString())
        EndLink.writeAnsi(this)
    }
}

/** Wrap this string in an OSC 8 clickable link. */
public fun String.link(
    uri: String,
    params: Map<String, String> = emptyMap(),
): LinkContent<String> = LinkContent(uri, this, params)

/**
 * Wrap this styled content in an OSC 8 clickable link.
 * Apply style extensions before calling [link] so the SGR codes sit
 * inside the OSC 8 boundaries.
 */
public fun <D> StyledContent<D>.link(
    uri: String,
    params: Map<String, String> = emptyMap(),
): LinkContent<StyledContent<D>> = LinkContent(uri, this, params)
