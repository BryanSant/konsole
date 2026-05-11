package tools.konsole.rich.prompt

import tools.konsole.rich.Console

/**
 * Yes / No prompt. Defaults to "n" if no default is given.
 *
 * ```
 * if (Confirm.ask("Continue?", default = true)) ...
 * ```
 */
public class Confirm(
    prompt: String,
    console: Console,
    default: Boolean? = null,
    showDefault: Boolean = true,
) : PromptBase<Boolean>(
    prompt = prompt,
    console = console,
    choices = listOf("y", "n"),
    default = default,
    showDefault = showDefault,
    showChoices = true,
    caseSensitive = false,
) {

    override fun process(value: String): Boolean = when (value.lowercase()) {
        "y", "yes", "true", "1" -> true
        "n", "no", "false", "0" -> false
        else -> throw InvalidResponse("Please enter y or n.")
    }

    public companion object {
        public fun ask(
            prompt: String,
            console: Console = Console.system(),
            default: Boolean? = null,
        ): Boolean = Confirm(prompt, console, default).ask()
    }
}
