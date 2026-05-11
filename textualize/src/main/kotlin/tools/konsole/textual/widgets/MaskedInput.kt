package tools.konsole.textual.widgets

/**
 * [Input] subclass that constrains user input to a fixed template.
 * Mirrors Python textual's `MaskedInput`.
 *
 * Template characters:
 * - `9` — any digit (`0-9`)
 * - `A` — any letter (`A-Z`, `a-z`)
 * - `*` — any character
 * - `H` — any hex digit (`0-9a-fA-F`)
 * - any other char — literal placeholder (auto-inserted, not user-typed)
 *
 * Example: `"999-99-9999"` for a US SSN; `"AA99 9999"` for a UK postcode;
 * `"HHHHHH"` for a 6-digit hex color.
 *
 * Validation: returns [ValidationResult.Invalid] when the buffer doesn't
 * fully match the template; [ValidationResult.Valid] once every variable
 * slot is filled.
 */
public open class MaskedInput(
    public val template: String,
    initial: String = "",
    placeholder: String = template.map { if (it.isMaskChar()) '_' else it }.joinToString(""),
    password: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Input(
    initial = initial,
    placeholder = placeholder,
    password = password,
    maxLength = template.length,
    validator = TemplateValidator(template),
    id = id,
    classes = classes,
) {

    override fun insert(text: String) {
        if (value.length >= template.length) return
        // Filter chars based on the next slot in the template.
        val sb = StringBuilder(value)
        for (ch in text) {
            val slot = template.getOrNull(sb.length) ?: break
            if (!slot.isMaskChar()) {
                // Auto-fill literal slots before inserting the typed char.
                sb.append(slot)
                if (template.getOrNull(sb.length)?.isMaskChar() != true) continue
            }
            val nextSlot = template[sb.length]
            if (matchesSlot(ch, nextSlot)) sb.append(ch) else continue
        }
        // Commit via super after pre-filtering.
        val current = value
        super.insert(sb.substring(current.length))
    }

    private fun matchesSlot(ch: Char, slot: Char): Boolean = when (slot) {
        '9' -> ch.isDigit()
        'A' -> ch.isLetter()
        '*' -> true
        'H' -> ch.isDigit() || ch in 'a'..'f' || ch in 'A'..'F'
        else -> ch == slot  // literal — only the literal char itself matches
    }

    private class TemplateValidator(private val template: String) : Validator {
        override fun validate(value: String): ValidationResult {
            if (value.length != template.length) {
                return ValidationResult.Invalid("expected ${template.length} chars, got ${value.length}")
            }
            for ((i, ch) in value.withIndex()) {
                val slot = template[i]
                val ok = when (slot) {
                    '9' -> ch.isDigit()
                    'A' -> ch.isLetter()
                    '*' -> true
                    'H' -> ch.isDigit() || ch in 'a'..'f' || ch in 'A'..'F'
                    else -> ch == slot
                }
                if (!ok) return ValidationResult.Invalid("position $i: '$ch' does not match template slot '$slot'")
            }
            return ValidationResult.Valid
        }
    }
}

private fun Char.isMaskChar(): Boolean = this == '9' || this == 'A' || this == '*' || this == 'H'
