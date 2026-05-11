package tools.konsole.textual.css

/**
 * A TCSS source token. Mirrors textual's `tokenizer.Token` (a NamedTuple in Python).
 *
 * Source positions are 1-indexed lines, 0-indexed columns — matching the convention
 * most editors use when reporting `(file, line, column)` triples.
 */
public data class Token(
    public val type: TokenType,
    public val value: String,
    public val file: String,
    public val line: Int,
    public val column: Int,
) {
    override fun toString(): String = "Token($type, ${value.take(40).let { "\"$it\"" }} @ $file:$line:$column)"
}

/** Token kinds emitted by [Tokenizer]. Mirrors textual's `Expect`-tagged token names. */
public enum class TokenType {
    Whitespace,
    Newline,
    Comment,
    Ident,
    Number,
    String,
    Hash,           // '#'
    Dot,            // '.'
    Colon,          // ':'
    Comma,          // ','
    Semicolon,      // ';'
    LeftBrace,      // '{'
    RightBrace,     // '}'
    LeftParen,      // '('
    RightParen,     // ')'
    Percent,        // '%'
    Fr,             // fractional unit (e.g. '1fr')
    Universal,      // '*'
    Hex,            // hex color like #ff00aa
    Ampersand,      // '&' nested-selector parent
    Plus,           // '+' adjacent-sibling combinator
    Greater,        // '>' child combinator
    Tilde,          // '~' general-sibling combinator
    Eof,
    Unknown,
}
