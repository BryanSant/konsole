package tools.konsole.rich.syntax

/** A typed slice of source: half-open `[start, end)` indices and a [TokenType]. */
public data class Token(public val start: Int, public val end: Int, public val type: TokenType)

/** Per-language tokenizer. Implementations return tokens for the whole input string. */
public fun interface Lexer {
    public fun tokenize(source: String): List<Token>

    public companion object {
        /** Find a registered lexer by language name (case-insensitive). */
        public fun forLanguage(name: String): Lexer? = LexerRegistry[name.lowercase()]

        /** Find a lexer by file extension (e.g., "kt", "py"). */
        public fun forExtension(ext: String): Lexer? = LexerRegistry.extensions[ext.lowercase()]
    }
}

/**
 * Regex-based tokenizer. Each rule is `(pattern, type)`; patterns are matched in declared
 * order at the current cursor — first match wins. Unmatched characters become single-char
 * [TokenType.TEXT] tokens.
 */
public class RegexLexer(private val rules: List<Rule>) : Lexer {

    public data class Rule(val pattern: Regex, val type: TokenType, val subTokens: List<TokenType>? = null)

    public constructor(vararg rules: Pair<Regex, TokenType>) : this(rules.map { Rule(it.first, it.second) })

    public companion object {
        public fun of(vararg rules: Rule): RegexLexer = RegexLexer(rules.toList())
    }

    override fun tokenize(source: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        val n = source.length
        var textStart = -1
        while (i < n) {
            var matched: MatchResult? = null
            var matchedType: TokenType? = null
            for (rule in rules) {
                val m = rule.pattern.matchAt(source, i)
                if (m != null && m.range.first == i && m.range.last >= i) {
                    matched = m
                    matchedType = rule.type
                    break
                }
            }
            if (matched != null) {
                if (textStart >= 0) {
                    out += Token(textStart, i, TokenType.TEXT)
                    textStart = -1
                }
                val end = matched.range.last + 1
                out += Token(i, end, matchedType!!)
                i = end
            } else {
                if (textStart < 0) textStart = i
                i += 1
            }
        }
        if (textStart >= 0) out += Token(textStart, n, TokenType.TEXT)
        return out
    }
}

internal object LexerRegistry {

    val extensions: Map<String, Lexer> = mapOf(
        "kt" to KotlinLexer,
        "kts" to KotlinLexer,
        "java" to JavaLexer,
        "py" to PythonLexer,
        "json" to JsonLexer,
        "sh" to BashLexer,
        "bash" to BashLexer,
        "sql" to SqlLexer,
        "yml" to YamlLexer,
        "yaml" to YamlLexer,
        "xml" to XmlLexer,
        "html" to HtmlLexer,
        "md" to MarkdownLexer,
        "markdown" to MarkdownLexer,
    )

    private val byName: Map<String, Lexer> = mapOf(
        "kotlin" to KotlinLexer,
        "kt" to KotlinLexer,
        "java" to JavaLexer,
        "python" to PythonLexer,
        "py" to PythonLexer,
        "json" to JsonLexer,
        "bash" to BashLexer,
        "sh" to BashLexer,
        "shell" to BashLexer,
        "sql" to SqlLexer,
        "yaml" to YamlLexer,
        "yml" to YamlLexer,
        "xml" to XmlLexer,
        "html" to HtmlLexer,
        "markdown" to MarkdownLexer,
        "md" to MarkdownLexer,
    )

    operator fun get(name: String): Lexer? = byName[name]
}
