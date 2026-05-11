package tools.konsole.textual.css

/**
 * Parse a TCSS source string into a list of [RuleSet]s.
 * Mirrors textual's `parse.parse` — recursive-descent over a Token stream.
 *
 * Syntax supported in Phase 8:
 *   - rule:           selector-set `{` declarations `}`
 *   - selector-set:   chain (`,` chain)*
 *   - chain:          compound (combinator compound)*
 *   - compound:       type? (`.` name | `#` name | `:` pseudo)*
 *   - combinator:     whitespace (descendant) | `>` (child)
 *   - declaration:    name `:` value `;`
 *
 * Comments are stripped during tokenisation. Unknown property values are
 * preserved as the raw value string so [StylesBuilder] can reject them.
 */
public object Parser {

    public fun parse(source: String, file: String = "<inline>"): List<RuleSet> {
        val tokens = Tokenizer(source, file).tokenize()
            .filter { it.type !in IGNORABLE }
            .toList()
        val state = State(tokens)
        val rules = mutableListOf<RuleSet>()
        while (state.peek().type != TokenType.Eof) {
            val rule = parseRule(state) ?: break
            rules += rule
        }
        return rules
    }

    private fun parseRule(state: State): RuleSet? {
        val selectorSet = parseSelectorSet(state) ?: return null
        state.expect(TokenType.LeftBrace) ?: return null
        val declarations = mutableListOf<Declaration>()
        while (state.peek().type !in setOf(TokenType.RightBrace, TokenType.Eof)) {
            val d = parseDeclaration(state)
            if (d != null) declarations += d
            else if (state.peek().type != TokenType.RightBrace) state.advance() // skip on error
        }
        state.expect(TokenType.RightBrace)
        return RuleSet(selectorSet, declarations)
    }

    private fun parseSelectorSet(state: State): SelectorSet? {
        val chains = mutableListOf<SelectorChain>()
        val first = parseSelectorChain(state) ?: return null
        chains += first
        while (state.peek().type == TokenType.Comma) {
            state.advance()
            val next = parseSelectorChain(state) ?: break
            chains += next
        }
        return SelectorSet(chains)
    }

    private fun parseSelectorChain(state: State): SelectorChain? {
        val selectors = mutableListOf<Selector>()
        var combinator = CombinatorType.Descendant
        while (true) {
            // Skip whitespace markers between selectors (filtered already, but the combinator
            // ' ' is implicit unless preceded by a `>` token).
            val sel = parseCompoundSelector(state, combinator) ?: break
            selectors += sel
            // Look for explicit combinator next
            val nxt = state.peek()
            combinator = when (nxt.type) {
                TokenType.Greater -> { state.advance(); CombinatorType.Child }
                TokenType.Plus -> { state.advance(); CombinatorType.AdjacentSibling }
                TokenType.Tilde -> { state.advance(); CombinatorType.GeneralSibling }
                TokenType.Ident, TokenType.Dot, TokenType.Hash, TokenType.Colon, TokenType.Universal -> CombinatorType.Descendant
                else -> break
            }
        }
        return if (selectors.isEmpty()) null else SelectorChain(selectors)
    }

    private fun parseCompoundSelector(state: State, combinator: CombinatorType): Selector? {
        val first = state.peek()
        var type: SelectorType
        var name: String
        when (first.type) {
            TokenType.Universal -> { state.advance(); type = SelectorType.Universal; name = "*" }
            TokenType.Ident -> { state.advance(); type = SelectorType.Type; name = first.value }
            TokenType.Dot -> {
                state.advance()
                val n = state.expect(TokenType.Ident) ?: return null
                type = SelectorType.Class; name = n.value
            }
            TokenType.Hash -> { state.advance(); type = SelectorType.Id; name = first.value.removePrefix("#") }
            TokenType.Colon -> {
                state.advance()
                val n = state.expect(TokenType.Ident) ?: return null
                type = SelectorType.PseudoClass; name = n.value
                // Standalone pseudo without a leading type ⇒ apply against universal selector.
                return Selector(SelectorType.Universal, "*", pseudoClasses = setOf(name), combinator = combinator)
            }
            else -> return null
        }
        // Collect trailing .class / #id / :pseudo modifiers on the same compound.
        val pseudo = mutableSetOf<String>()
        val extraClasses = mutableSetOf<String>()
        var extraId: String? = null
        loop@ while (true) {
            when (state.peek().type) {
                TokenType.Dot -> {
                    state.advance()
                    val n = state.expect(TokenType.Ident) ?: break@loop
                    extraClasses += n.value
                }
                TokenType.Hash -> {
                    val n = state.advance()
                    extraId = n.value.removePrefix("#")
                }
                TokenType.Colon -> {
                    state.advance()
                    val n = state.expect(TokenType.Ident) ?: break@loop
                    pseudo += n.value
                }
                else -> break@loop
            }
        }
        // Compose: if we have extra classes/id, emit the union as a single compound by overwriting
        // type=Type. The classes are returned as pseudoClasses-shaped extras for simplicity.
        return if (extraClasses.isEmpty() && extraId == null) {
            Selector(type, name, pseudo, combinator)
        } else {
            // Phase-8 simplification: pack classes/id into pseudo set so matching short-circuits
            // on Selector.matches() if any compound piece misses. A future Selector model can
            // model compounds natively. For now we just AND classes into pseudo where they're
            // checked alongside the original name match.
            Selector(type, name, pseudo + extraClasses.map { ".$it" } + listOfNotNull(extraId?.let { "#$it" }), combinator)
        }
    }

    private fun parseDeclaration(state: State): Declaration? {
        val nameTok = state.expect(TokenType.Ident) ?: return null
        state.expect(TokenType.Colon) ?: return null
        val valueBuf = StringBuilder()
        var important = false
        while (state.peek().type !in setOf(TokenType.Semicolon, TokenType.RightBrace, TokenType.Eof)) {
            val t = state.advance()
            if (t.type == TokenType.Ident && t.value == "important") {
                important = true
            } else {
                if (valueBuf.isNotEmpty() && t.type !in setOf(TokenType.LeftParen, TokenType.RightParen)) valueBuf.append(' ')
                valueBuf.append(t.value)
            }
        }
        if (state.peek().type == TokenType.Semicolon) state.advance()
        return Declaration(nameTok.value, valueBuf.toString().trim(), important, nameTok.line)
    }

    private val IGNORABLE = setOf(TokenType.Whitespace, TokenType.Newline, TokenType.Comment)

    private class State(private val tokens: List<Token>) {
        private var i = 0
        fun peek(): Token = if (i < tokens.size) tokens[i] else tokens.last()
        fun advance(): Token { val t = peek(); if (i < tokens.size) i += 1; return t }
        fun expect(type: TokenType): Token? = if (peek().type == type) advance() else null
    }
}
