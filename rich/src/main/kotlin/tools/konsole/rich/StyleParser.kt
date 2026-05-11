package tools.konsole.rich

import tools.konsole.core.style.Color

public object StyleParser {

    public fun parse(input: String): Style {
        val tokens = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return Style.NULL

        var style = Style.NULL
        var colorAssigned = false
        var i = 0
        while (i < tokens.size) {
            val tok = tokens[i]
            when {
                tok == "not" -> {
                    require(i + 1 < tokens.size) { "Expected attribute after 'not' in style: '$input'" }
                    val next = tokens[i + 1]
                    style = applyAttribute(style, next, false)
                        ?: error("Unknown attribute after 'not': '$next' in style: '$input'")
                    i += 2
                }

                tok == "on" -> {
                    require(i + 1 < tokens.size) { "Expected color after 'on' in style: '$input'" }
                    val next = tokens[i + 1]
                    val color = Color.parse(next)
                        ?: error("Unknown color after 'on': '$next' in style: '$input'")
                    style = style.copy(bgcolor = color)
                    i += 2
                }

                tok.startsWith("link=") -> {
                    style = style.copy(link = tok.removePrefix("link="))
                    i += 1
                }

                else -> {
                    val asAttr = applyAttribute(style, tok, true)
                    if (asAttr != null) {
                        style = asAttr
                        i += 1
                    } else {
                        val color = Color.parse(tok)
                        if (color != null) {
                            check(!colorAssigned) {
                                "Style has more than one foreground color: '$input'"
                            }
                            style = style.copy(color = color)
                            colorAssigned = true
                            i += 1
                        } else {
                            error("Unknown style token: '$tok' in '$input'")
                        }
                    }
                }
            }
        }
        return style
    }

    private fun applyAttribute(style: Style, name: String, value: Boolean): Style? = when (name) {
        "bold", "b" -> style.copy(bold = value)
        "italic", "i" -> style.copy(italic = value)
        "underline", "u" -> style.copy(underline = value)
        "dim", "d" -> style.copy(dim = value)
        "reverse", "r" -> style.copy(reverse = value)
        "strike", "s" -> style.copy(strike = value)
        "blink" -> style.copy(blink = value)
        "blink2" -> style.copy(blink2 = value)
        "conceal" -> style.copy(conceal = value)
        "frame" -> style.copy(frame = value)
        "encircle" -> style.copy(encircle = value)
        "overline", "o" -> style.copy(overline = value)
        else -> null
    }
}
