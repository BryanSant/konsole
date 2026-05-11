package tools.konsole.rich.syntax

import tools.konsole.rich.Style
import tools.konsole.core.style.Color

/** Categories of source-code tokens that a [Lexer] can emit. */
public enum class TokenType {
    KEYWORD,
    KEYWORD_TYPE,
    KEYWORD_CONSTANT,
    BUILTIN,
    STRING,
    STRING_INTERPOLATION,
    STRING_ESCAPE,
    DOCSTRING,
    COMMENT,
    NUMBER,
    OPERATOR,
    PUNCTUATION,
    IDENTIFIER,
    TYPE,
    FUNCTION,
    CLASS,
    DECORATOR,
    ANNOTATION,
    PROPERTY,
    PARAMETER,
    NAMESPACE,
    NULL,
    BOOL,
    REGEX,
    HTML_TAG,
    HTML_ATTR,
    YAML_KEY,
    SQL_KEYWORD,
    LINE_NUMBER,
    /** Unrecognized / fallback. */
    TEXT,
}

/** Maps [TokenType]s to [Style]s and provides a default background. */
public class SyntaxTheme(
    public val name: String,
    public val styles: Map<TokenType, Style>,
    public val background: Style? = null,
    public val highlight: Style? = null,
) {
    public operator fun get(type: TokenType): Style = styles[type] ?: Style.NULL

    public companion object {

        /** ANSI-only theme — uses named colors so it works in 16-color terminals. */
        public val ANSI_DARK: SyntaxTheme = SyntaxTheme(
            name = "ansi_dark",
            styles = mapOf(
                TokenType.KEYWORD to Style(color = Color.Magenta, bold = true),
                TokenType.KEYWORD_TYPE to Style(color = Color.Cyan, bold = true),
                TokenType.KEYWORD_CONSTANT to Style(color = Color.Yellow, bold = true),
                TokenType.BUILTIN to Style(color = Color.Cyan),
                TokenType.STRING to Style(color = Color.Green),
                TokenType.STRING_INTERPOLATION to Style(color = Color.Yellow),
                TokenType.STRING_ESCAPE to Style(color = Color.Yellow, bold = true),
                TokenType.DOCSTRING to Style(color = Color.Green, italic = true),
                TokenType.COMMENT to Style(color = Color.Grey, italic = true),
                TokenType.NUMBER to Style(color = Color.Cyan),
                TokenType.OPERATOR to Style(color = Color.Magenta),
                TokenType.PUNCTUATION to Style.NULL,
                TokenType.IDENTIFIER to Style.NULL,
                TokenType.TYPE to Style(color = Color.Cyan, bold = true),
                TokenType.FUNCTION to Style(color = Color.Yellow),
                TokenType.CLASS to Style(color = Color.Yellow, bold = true),
                TokenType.DECORATOR to Style(color = Color.Yellow),
                TokenType.ANNOTATION to Style(color = Color.Yellow),
                TokenType.PROPERTY to Style(color = Color.Cyan),
                TokenType.PARAMETER to Style(color = Color.Yellow),
                TokenType.NAMESPACE to Style(color = Color.Cyan),
                TokenType.NULL to Style(color = Color.Yellow, italic = true),
                TokenType.BOOL to Style(color = Color.Yellow, italic = true),
                TokenType.REGEX to Style(color = Color.Magenta),
                TokenType.HTML_TAG to Style(color = Color.Blue),
                TokenType.HTML_ATTR to Style(color = Color.Cyan),
                TokenType.YAML_KEY to Style(color = Color.Cyan),
                TokenType.SQL_KEYWORD to Style(color = Color.Magenta, bold = true),
                TokenType.LINE_NUMBER to Style(color = Color.DarkGrey),
                TokenType.TEXT to Style.NULL,
            ),
        )

        public val ANSI_LIGHT: SyntaxTheme = SyntaxTheme(
            name = "ansi_light",
            styles = mapOf(
                TokenType.KEYWORD to Style(color = Color.DarkMagenta, bold = true),
                TokenType.KEYWORD_TYPE to Style(color = Color.DarkBlue, bold = true),
                TokenType.KEYWORD_CONSTANT to Style(color = Color.DarkRed, bold = true),
                TokenType.BUILTIN to Style(color = Color.DarkCyan),
                TokenType.STRING to Style(color = Color.DarkGreen),
                TokenType.STRING_INTERPOLATION to Style(color = Color.DarkYellow),
                TokenType.STRING_ESCAPE to Style(color = Color.DarkRed, bold = true),
                TokenType.DOCSTRING to Style(color = Color.DarkGreen, italic = true),
                TokenType.COMMENT to Style(color = Color.DarkGrey, italic = true),
                TokenType.NUMBER to Style(color = Color.DarkCyan),
                TokenType.OPERATOR to Style(color = Color.DarkMagenta),
                TokenType.PUNCTUATION to Style.NULL,
                TokenType.TYPE to Style(color = Color.DarkBlue, bold = true),
                TokenType.FUNCTION to Style(color = Color.DarkYellow),
                TokenType.CLASS to Style(color = Color.DarkRed, bold = true),
                TokenType.DECORATOR to Style(color = Color.DarkYellow),
                TokenType.ANNOTATION to Style(color = Color.DarkYellow),
                TokenType.PROPERTY to Style(color = Color.DarkCyan),
                TokenType.PARAMETER to Style(color = Color.DarkYellow),
                TokenType.NAMESPACE to Style(color = Color.DarkCyan),
                TokenType.NULL to Style(color = Color.DarkRed, italic = true),
                TokenType.BOOL to Style(color = Color.DarkRed, italic = true),
                TokenType.REGEX to Style(color = Color.DarkMagenta),
                TokenType.HTML_TAG to Style(color = Color.DarkBlue),
                TokenType.HTML_ATTR to Style(color = Color.DarkCyan),
                TokenType.YAML_KEY to Style(color = Color.DarkBlue),
                TokenType.SQL_KEYWORD to Style(color = Color.DarkMagenta, bold = true),
                TokenType.LINE_NUMBER to Style(color = Color.Grey),
                TokenType.TEXT to Style.NULL,
                TokenType.IDENTIFIER to Style.NULL,
            ),
        )

        /** Truecolor Monokai — the rich default. */
        public val MONOKAI: SyntaxTheme = SyntaxTheme(
            name = "monokai",
            styles = mapOf(
                TokenType.KEYWORD to Style(color = Color.Rgb(249, 38, 114), bold = true),
                TokenType.KEYWORD_TYPE to Style(color = Color.Rgb(102, 217, 239), italic = true),
                TokenType.KEYWORD_CONSTANT to Style(color = Color.Rgb(174, 129, 255)),
                TokenType.BUILTIN to Style(color = Color.Rgb(102, 217, 239)),
                TokenType.STRING to Style(color = Color.Rgb(230, 219, 116)),
                TokenType.STRING_INTERPOLATION to Style(color = Color.Rgb(166, 226, 46)),
                TokenType.STRING_ESCAPE to Style(color = Color.Rgb(174, 129, 255)),
                TokenType.DOCSTRING to Style(color = Color.Rgb(115, 115, 115), italic = true),
                TokenType.COMMENT to Style(color = Color.Rgb(117, 113, 94), italic = true),
                TokenType.NUMBER to Style(color = Color.Rgb(174, 129, 255)),
                TokenType.OPERATOR to Style(color = Color.Rgb(249, 38, 114)),
                TokenType.PUNCTUATION to Style(color = Color.Rgb(248, 248, 242)),
                TokenType.IDENTIFIER to Style(color = Color.Rgb(248, 248, 242)),
                TokenType.TYPE to Style(color = Color.Rgb(166, 226, 46)),
                TokenType.FUNCTION to Style(color = Color.Rgb(166, 226, 46)),
                TokenType.CLASS to Style(color = Color.Rgb(166, 226, 46), bold = true),
                TokenType.DECORATOR to Style(color = Color.Rgb(166, 226, 46), italic = true),
                TokenType.ANNOTATION to Style(color = Color.Rgb(166, 226, 46), italic = true),
                TokenType.PROPERTY to Style(color = Color.Rgb(102, 217, 239)),
                TokenType.PARAMETER to Style(color = Color.Rgb(253, 151, 31), italic = true),
                TokenType.NAMESPACE to Style(color = Color.Rgb(102, 217, 239)),
                TokenType.NULL to Style(color = Color.Rgb(174, 129, 255), italic = true),
                TokenType.BOOL to Style(color = Color.Rgb(174, 129, 255), italic = true),
                TokenType.REGEX to Style(color = Color.Rgb(230, 219, 116)),
                TokenType.HTML_TAG to Style(color = Color.Rgb(249, 38, 114)),
                TokenType.HTML_ATTR to Style(color = Color.Rgb(166, 226, 46)),
                TokenType.YAML_KEY to Style(color = Color.Rgb(249, 38, 114)),
                TokenType.SQL_KEYWORD to Style(color = Color.Rgb(249, 38, 114), bold = true),
                TokenType.LINE_NUMBER to Style(color = Color.Rgb(70, 70, 70)),
                TokenType.TEXT to Style(color = Color.Rgb(248, 248, 242)),
            ),
            background = Style(bgcolor = Color.Rgb(39, 40, 34)),
        )

        /** GitHub light. */
        public val GITHUB: SyntaxTheme = SyntaxTheme(
            name = "github",
            styles = mapOf(
                TokenType.KEYWORD to Style(color = Color.Rgb(207, 34, 46)),
                TokenType.KEYWORD_TYPE to Style(color = Color.Rgb(149, 33, 33)),
                TokenType.KEYWORD_CONSTANT to Style(color = Color.Rgb(5, 80, 174)),
                TokenType.BUILTIN to Style(color = Color.Rgb(149, 33, 33)),
                TokenType.STRING to Style(color = Color.Rgb(10, 49, 105)),
                TokenType.COMMENT to Style(color = Color.Rgb(106, 115, 125), italic = true),
                TokenType.NUMBER to Style(color = Color.Rgb(5, 80, 174)),
                TokenType.OPERATOR to Style(color = Color.Rgb(207, 34, 46)),
                TokenType.PUNCTUATION to Style(color = Color.Rgb(36, 41, 47)),
                TokenType.IDENTIFIER to Style(color = Color.Rgb(36, 41, 47)),
                TokenType.TYPE to Style(color = Color.Rgb(149, 33, 33)),
                TokenType.FUNCTION to Style(color = Color.Rgb(98, 60, 152)),
                TokenType.CLASS to Style(color = Color.Rgb(149, 33, 33), bold = true),
                TokenType.DECORATOR to Style(color = Color.Rgb(149, 33, 33)),
                TokenType.ANNOTATION to Style(color = Color.Rgb(149, 33, 33)),
                TokenType.PROPERTY to Style(color = Color.Rgb(36, 41, 47)),
                TokenType.NAMESPACE to Style(color = Color.Rgb(149, 33, 33)),
                TokenType.NULL to Style(color = Color.Rgb(5, 80, 174)),
                TokenType.BOOL to Style(color = Color.Rgb(5, 80, 174)),
                TokenType.STRING_ESCAPE to Style(color = Color.Rgb(149, 33, 33)),
                TokenType.LINE_NUMBER to Style(color = Color.Rgb(170, 170, 170)),
                TokenType.TEXT to Style(color = Color.Rgb(36, 41, 47)),
            ),
        )

        public val ALL: List<SyntaxTheme> = listOf(MONOKAI, ANSI_DARK, ANSI_LIGHT, GITHUB)

        public operator fun get(name: String): SyntaxTheme =
            ALL.firstOrNull { it.name == name } ?: error("Unknown syntax theme: '$name'. Known: ${ALL.map { it.name }}")
    }
}
