package tools.konsole.rich.prompt

import tools.konsole.rich.Console

public class IntPrompt(
    prompt: String,
    console: Console,
    choices: List<String>? = null,
    default: Int? = null,
    showDefault: Boolean = true,
) : PromptBase<Int>(prompt, console, choices, default, showDefault) {

    override fun process(value: String): Int = value.toIntOrNull()
        ?: throw InvalidResponse("Please enter a valid integer.")

    public companion object {
        public fun ask(
            prompt: String,
            console: Console = Console.system(),
            default: Int? = null,
        ): Int = IntPrompt(prompt, console, default = default).ask()
    }
}
