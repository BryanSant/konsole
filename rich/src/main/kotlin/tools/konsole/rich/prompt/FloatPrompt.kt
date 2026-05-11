package tools.konsole.rich.prompt

import tools.konsole.rich.Console

public open class FloatPrompt(
    prompt: String,
    console: Console,
    choices: List<String>? = null,
    default: Double? = null,
    showDefault: Boolean = true,
) : PromptBase<Double>(prompt, console, choices, default, showDefault) {

    override fun process(value: String): Double = value.toDoubleOrNull()
        ?: throw InvalidResponse("Please enter a valid number.")

    public companion object {
        public fun ask(
            prompt: String,
            console: Console = Console.system(),
            default: Double? = null,
        ): Double = FloatPrompt(prompt, console, default = default).ask()
    }
}
