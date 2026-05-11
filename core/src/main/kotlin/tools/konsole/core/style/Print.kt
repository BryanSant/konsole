package tools.konsole.core.style

import tools.konsole.core.Command

/**
 * Emits the `toString()` of [value] verbatim. The simplest command — useful
 * for inserting plain text mid-stream alongside cursor / style commands.
 *
 * If [value] is a [StyledContent], its own `toString()` already wraps the
 * content in SGR open/reset, so styled text composes naturally:
 *
 * ```
 * out.execute(Print("hello ".bold()), Print("world".red()))
 * ```
 */
public data class Print<T>(val value: T) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(value.toString())
    }
}
