package tools.konsole.rich.prompt

import tools.konsole.rich.Console
import tools.konsole.rich.PrintOptions
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Interactive line-based prompt. Mirrors `rich.prompt.Prompt`.
 *
 * Subclass and override [process] to coerce or validate input. Use [ask] to read until valid.
 */
public open class PromptBase<T>(
    public val prompt: String,
    public val console: Console,
    public val choices: List<String>? = null,
    public val default: T? = null,
    public val showDefault: Boolean = true,
    public val showChoices: Boolean = true,
    public val caseSensitive: Boolean = false,
    public val password: Boolean = false,
) {

    private val reader: BufferedReader = BufferedReader(InputStreamReader(System.`in`))

    /** Convert raw user input into a value of type T or throw [InvalidResponse] with a message. */
    protected open fun process(value: String): T {
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    /** Validate a parsed value (e.g. choice membership). Default: accept everything. */
    protected open fun validate(value: T): T = value

    /** Render the prompt line, then read a value from stdin. Loops on invalid input. */
    public fun ask(): T {
        while (true) {
            renderPrompt()
            val line = reader.readLine()?.trim() ?: ""
            val raw = if (line.isEmpty() && default != null) default.toString() else line
            try {
                val parsed = process(raw)
                val finalValue = validate(parsed)
                if (choices != null && !inChoices(raw)) {
                    invalidChoice(raw)
                    continue
                }
                return finalValue
            } catch (e: InvalidResponse) {
                console.print(Text(e.message ?: "Invalid response.", style = console.theme["prompt.invalid"] ?: Style(color = tools.konsole.core.style.Color.Red)))
            }
        }
    }

    private fun inChoices(value: String): Boolean {
        val cs = choices ?: return true
        return if (caseSensitive) value in cs else cs.any { it.equals(value, ignoreCase = true) }
    }

    private fun invalidChoice(value: String) {
        val styled = Text("Please select one of: ${choices?.joinToString(", ")}.", style = console.theme["prompt.invalid.choice"] ?: Style(color = tools.konsole.core.style.Color.Red))
        console.print(styled)
    }

    private fun renderPrompt() {
        val parts = StringBuilder(prompt)
        if (showChoices && !choices.isNullOrEmpty()) {
            parts.append(" \\[")
            parts.append(choices.joinToString("/"))
            parts.append("]")
        }
        if (showDefault && default != null) {
            parts.append(" (")
            parts.append(default.toString())
            parts.append(")")
        }
        parts.append(": ")
        console.print(Markup.parse(parts.toString()), PrintOptions(end = ""))
        console.writer.flush()
    }
}

/** A response could not be parsed/validated. Triggers a re-prompt loop in [PromptBase.ask]. */
public class InvalidResponse(message: String) : RuntimeException(message)

/**
 * String prompt with optional choices.
 *
 * ```
 * val name: String = Prompt.ask("What is your name?", default = "world")
 * ```
 */
public class Prompt(
    prompt: String,
    console: Console,
    choices: List<String>? = null,
    default: String? = null,
    showDefault: Boolean = true,
    showChoices: Boolean = true,
    caseSensitive: Boolean = false,
    password: Boolean = false,
) : PromptBase<String>(prompt, console, choices, default, showDefault, showChoices, caseSensitive, password) {

    override fun process(value: String): String = value

    public companion object {
        public fun ask(
            prompt: String,
            console: Console = Console.system(),
            choices: List<String>? = null,
            default: String? = null,
        ): String = Prompt(prompt, console, choices, default).ask()
    }
}
