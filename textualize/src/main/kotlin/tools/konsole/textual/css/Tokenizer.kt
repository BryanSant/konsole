package tools.konsole.textual.css

/**
 * Hand-written lexer for the TCSS dialect — mirrors textual's `tokenize.py` machinery.
 *
 * Emits a `Sequence<Token>` with `(file, line, column)` tagged on each token so
 * downstream error reporting and future hot-reload can show precise locations.
 *
 * The tokeniser is forward-only and stateless past its `pos` cursor — invoke
 * [tokenize] for each new source. Pass a `file` label (the TCSS file name or
 * `"<inline>"`) to embed in token positions.
 */
public class Tokenizer(public val source: String, public val file: String = "<inline>") {

    private var pos: Int = 0
    private var line: Int = 1
    private var column: Int = 0

    public fun tokenize(): Sequence<Token> = sequence {
        while (pos < source.length) {
            val ch = source[pos]
            val startLine = line
            val startCol = column
            when {
                ch == '\n' -> {
                    yield(emit(TokenType.Newline, "\n", startLine, startCol))
                    consume(1)
                }
                ch.isWhitespace() -> {
                    val s = readWhile { it.isWhitespace() && it != '\n' }
                    yield(emit(TokenType.Whitespace, s, startLine, startCol))
                }
                ch == '/' && peek(1) == '*' -> {
                    val s = readBlockComment()
                    yield(emit(TokenType.Comment, s, startLine, startCol))
                }
                ch == '/' && peek(1) == '/' -> {
                    val s = readWhile { it != '\n' }
                    yield(emit(TokenType.Comment, s, startLine, startCol))
                }
                ch == '#' -> {
                    consume(1)
                    val rest = readWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
                    // Hex color if all chars are hex digits and length is 3, 4, 6, or 8.
                    val isHex = rest.isNotEmpty() && rest.length in setOf(3, 4, 6, 8) && rest.all { it.isHexDigit() }
                    val kind = if (isHex) TokenType.Hex else TokenType.Hash
                    yield(emit(kind, "#$rest", startLine, startCol))
                }
                ch == '.' && peek(1)?.isDigit() != true -> { yield(emit(TokenType.Dot, ".", startLine, startCol)); consume(1) }
                ch == ':' -> { yield(emit(TokenType.Colon, ":", startLine, startCol)); consume(1) }
                ch == ',' -> { yield(emit(TokenType.Comma, ",", startLine, startCol)); consume(1) }
                ch == ';' -> { yield(emit(TokenType.Semicolon, ";", startLine, startCol)); consume(1) }
                ch == '{' -> { yield(emit(TokenType.LeftBrace, "{", startLine, startCol)); consume(1) }
                ch == '}' -> { yield(emit(TokenType.RightBrace, "}", startLine, startCol)); consume(1) }
                ch == '(' -> { yield(emit(TokenType.LeftParen, "(", startLine, startCol)); consume(1) }
                ch == ')' -> { yield(emit(TokenType.RightParen, ")", startLine, startCol)); consume(1) }
                ch == '%' -> { yield(emit(TokenType.Percent, "%", startLine, startCol)); consume(1) }
                ch == '*' -> { yield(emit(TokenType.Universal, "*", startLine, startCol)); consume(1) }
                ch == '&' -> { yield(emit(TokenType.Ampersand, "&", startLine, startCol)); consume(1) }
                ch == '+' -> { yield(emit(TokenType.Plus, "+", startLine, startCol)); consume(1) }
                ch == '>' -> { yield(emit(TokenType.Greater, ">", startLine, startCol)); consume(1) }
                ch == '~' -> { yield(emit(TokenType.Tilde, "~", startLine, startCol)); consume(1) }
                ch == '"' || ch == '\'' -> {
                    val s = readString(ch)
                    yield(emit(TokenType.String, s, startLine, startCol))
                }
                ch.isDigit() || (ch == '.' && peek(1)?.isDigit() == true) || (ch == '-' && peek(1)?.isDigit() == true) -> {
                    val (num, isFr) = readNumber()
                    if (isFr) yield(emit(TokenType.Fr, num, startLine, startCol))
                    else yield(emit(TokenType.Number, num, startLine, startCol))
                }
                ch.isLetter() || ch == '_' || ch == '-' -> {
                    val s = readWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
                    yield(emit(TokenType.Ident, s, startLine, startCol))
                }
                else -> {
                    yield(emit(TokenType.Unknown, ch.toString(), startLine, startCol))
                    consume(1)
                }
            }
        }
        yield(Token(TokenType.Eof, "", file, line, column))
    }

    private fun consume(n: Int) {
        repeat(n) {
            if (pos < source.length) {
                if (source[pos] == '\n') { line += 1; column = 0 }
                else column += 1
                pos += 1
            }
        }
    }

    private fun emit(type: TokenType, value: String, ln: Int, col: Int): Token =
        Token(type, value, file, ln, col)

    private inline fun readWhile(predicate: (Char) -> Boolean): String {
        val start = pos
        while (pos < source.length && predicate(source[pos])) consume(1)
        return source.substring(start, pos)
    }

    private fun readBlockComment(): String {
        val start = pos
        consume(2) // /*
        while (pos < source.length) {
            if (source[pos] == '*' && peek(1) == '/') {
                consume(2); break
            }
            consume(1)
        }
        return source.substring(start, pos)
    }

    private fun readString(quote: Char): String {
        val start = pos
        consume(1) // opening quote
        while (pos < source.length && source[pos] != quote) {
            if (source[pos] == '\\' && pos + 1 < source.length) consume(2)
            else consume(1)
        }
        if (pos < source.length) consume(1) // closing quote
        return source.substring(start, pos)
    }

    private fun readNumber(): Pair<String, Boolean> {
        val start = pos
        if (source[pos] == '-') consume(1)
        while (pos < source.length && (source[pos].isDigit() || source[pos] == '.')) consume(1)
        // Optional 'fr' suffix (no space)
        var fr = false
        if (pos + 1 < source.length && source[pos] == 'f' && source[pos + 1] == 'r') {
            fr = true
            consume(2)
        }
        val s = source.substring(start, pos)
        return s to fr
    }

    private fun peek(offset: Int): Char? =
        if (pos + offset < source.length) source[pos + offset] else null
}

private fun Char.isHexDigit(): Boolean = isDigit() || this in 'a'..'f' || this in 'A'..'F'
