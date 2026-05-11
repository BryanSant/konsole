package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/**
 * Single-line text input. Mirrors Python textual's `Input` widget.
 *
 * Maintains a buffer and cursor position; emits [Changed] on every edit
 * and [Submitted] when the user presses Enter. Supports placeholder text
 * shown when the buffer is empty, an optional [Validator] enforcing rules,
 * and an optional [Suggester] for autocompletion hints.
 *
 * Common bindings:
 * - left/right        — move cursor by one
 * - home/end          — jump to start/end
 * - backspace/delete  — delete chars before/after cursor
 * - enter             — submit
 *
 * Modifying [value] (via [insert]/[deleteChar]/[clear]) refreshes the widget
 * and fires [Changed]; [Validator.validate] runs against each new value and
 * exposes the result via [validationResult].
 */
public open class Input(
    initial: String = "",
    public val placeholder: String = "",
    public val password: Boolean = false,
    public val maxLength: Int? = null,
    public val validator: Validator? = null,
    public val suggester: Suggester? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var value: String = initial
        private set

    public var cursor: Int = initial.length
        private set

    public var validationResult: ValidationResult = ValidationResult.Valid
        private set

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("left", "cursor_left", show = false),
            Binding("right", "cursor_right", show = false),
            Binding("home", "cursor_home", show = false),
            Binding("end", "cursor_end", show = false),
            Binding("backspace", "delete_left", show = false),
            Binding("delete", "delete_right", show = false),
            Binding("enter", "submit"),
        )
    )

    /** Insert [text] at the cursor. Fires [Changed]. */
    public open fun insert(text: String) {
        if (maxLength != null && value.length + text.length > maxLength) return
        value = value.substring(0, cursor) + text + value.substring(cursor)
        cursor += text.length
        validateAndNotify()
    }

    /** Delete one char before the cursor. */
    public open fun deleteLeft() {
        if (cursor == 0) return
        value = value.substring(0, cursor - 1) + value.substring(cursor)
        cursor -= 1
        validateAndNotify()
    }

    /** Delete one char after the cursor. */
    public open fun deleteRight() {
        if (cursor >= value.length) return
        value = value.substring(0, cursor) + value.substring(cursor + 1)
        validateAndNotify()
    }

    public fun moveCursor(offset: Int) {
        cursor = (cursor + offset).coerceIn(0, value.length)
        refresh()
    }

    public fun moveCursorTo(index: Int) {
        cursor = index.coerceIn(0, value.length)
        refresh()
    }

    public open fun clear() {
        if (value.isEmpty()) return
        value = ""
        cursor = 0
        validateAndNotify()
    }

    /** Programmatic submission — emits [Submitted] and triggers final validation. */
    public open fun submit(): Boolean {
        validateAndNotify(notifyChange = false)
        return post(Submitted(this, value, validationResult))
    }

    private fun validateAndNotify(notifyChange: Boolean = true) {
        validationResult = validator?.validate(value) ?: ValidationResult.Valid
        if (notifyChange) post(Changed(this, value, validationResult))
        refresh()
    }

    override fun render(): Renderable {
        val displayed = if (password) "•".repeat(value.length) else value
        val style = if (hasFocus) Style(color = Color.White, bgcolor = Color.Rgb(0x2d, 0x32, 0x3f))
                    else Style(color = Color.White, bgcolor = Color.Rgb(0x1f, 0x22, 0x29))
        val text = Text()
        if (displayed.isEmpty() && placeholder.isNotEmpty()) {
            text.append(placeholder, style + Style(dim = true, italic = true))
        } else {
            // Render value with cursor highlight when focused.
            if (hasFocus && cursor in 0..displayed.length) {
                text.append(displayed.substring(0, cursor), style)
                val cursorChar = if (cursor < displayed.length) displayed.substring(cursor, cursor + 1) else " "
                text.append(cursorChar, Style(color = Color.Black, bgcolor = Color.White))
                if (cursor + 1 < displayed.length) text.append(displayed.substring(cursor + 1), style)
                else if (cursor >= displayed.length) text.append("", style)
            } else {
                text.append(displayed, style)
            }
        }
        // Suggestion ghost text
        if (hasFocus && displayed.isNotEmpty()) {
            suggester?.suggest(value)?.let { hint ->
                if (hint.startsWith(value) && hint.length > value.length) {
                    text.append(hint.substring(value.length), style + Style(dim = true, italic = true))
                }
            }
        }
        return text
    }

    /** Emitted whenever [value] changes. */
    public data class Changed(val input: Input, val value: String, val validationResult: ValidationResult) : Message()

    /** Emitted when the user presses Enter (or [submit] is called programmatically). */
    public data class Submitted(val input: Input, val value: String, val validationResult: ValidationResult) : Message()
}

/**
 * Validates an [Input]'s value. Mirrors Python textual's `Validator` ABC.
 * Return [ValidationResult.Valid] to accept, [ValidationResult.Invalid] with
 * a reason to reject — the input is still accepted into the buffer (matching
 * textual semantics) but the reason is exposed via `validationResult` and
 * downstream widgets can react.
 */
public fun interface Validator {
    public fun validate(value: String): ValidationResult
}

public sealed interface ValidationResult {
    public data object Valid : ValidationResult
    public data class Invalid(val reason: String) : ValidationResult
    public val isValid: Boolean get() = this is Valid
}

/** Autocompletion hint provider. Mirrors textual's `Suggester` ABC. */
public fun interface Suggester {
    /** Return a suggestion that starts with [value], or `null` for no hint. */
    public fun suggest(value: String): String?
}

/** Simple prefix-match suggester over a fixed corpus. */
public class WordListSuggester(private val words: List<String>, private val caseSensitive: Boolean = false) : Suggester {
    override fun suggest(value: String): String? {
        if (value.isEmpty()) return null
        return words.firstOrNull { word ->
            if (caseSensitive) word.startsWith(value)
            else word.startsWith(value, ignoreCase = true)
        }
    }
}

/** Built-in length validator. */
public class LengthValidator(private val min: Int = 0, private val max: Int = Int.MAX_VALUE) : Validator {
    override fun validate(value: String): ValidationResult = when {
        value.length < min -> ValidationResult.Invalid("must be at least $min characters")
        value.length > max -> ValidationResult.Invalid("must be at most $max characters")
        else -> ValidationResult.Valid
    }
}

/** Built-in regex validator. */
public class RegexValidator(private val pattern: Regex, private val message: String = "format mismatch") : Validator {
    public constructor(pattern: String, message: String = "format mismatch") : this(Regex(pattern), message)
    override fun validate(value: String): ValidationResult =
        if (pattern.matches(value)) ValidationResult.Valid else ValidationResult.Invalid(message)
}

/** Built-in integer validator. */
public object IntegerValidator : Validator {
    override fun validate(value: String): ValidationResult =
        if (value.toIntOrNull() != null || value.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid("not a valid integer")
}

/** Built-in numeric (Double) validator. */
public object NumberValidator : Validator {
    override fun validate(value: String): ValidationResult =
        if (value.toDoubleOrNull() != null || value.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid("not a valid number")
}
