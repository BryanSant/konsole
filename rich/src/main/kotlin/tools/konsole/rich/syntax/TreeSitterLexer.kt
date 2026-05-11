package tools.konsole.rich.syntax

import org.treesitter.TSLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser

/**
 * [Lexer] backed by a tree-sitter grammar. Provides far more accurate
 * tokenization than the regex-based lexers for complex syntaxes (string
 * interpolation, nested templates, embedded languages, …) at the cost of
 * shipping a per-language native library (~5MB each, bundled with the
 * `io.github.bonede:tree-sitter-<lang>` artifacts).
 *
 * Maps tree-sitter node `type` strings to konsole's [TokenType] via the
 * supplied [tokenTypeMap]; node types not in the map fall back to
 * [TokenType.TEXT]. Tree-sitter operates in UTF-8 byte offsets while our
 * tokens use Kotlin `String` char indices, so a char↔byte index map is
 * computed once per `tokenize` call.
 *
 * Construction is cheap; the underlying [TSParser] is created on first use
 * and reused. The class is intentionally not thread-safe — callers needing
 * concurrent use should pool instances or wrap in a mutex.
 */
public class TreeSitterLexer(
    private val language: TSLanguage,
    private val tokenTypeMap: Map<String, TokenType>,
    private val defaultType: TokenType = TokenType.TEXT,
) : Lexer {

    private val parser: TSParser by lazy {
        TSParser().apply { language = this@TreeSitterLexer.language }
    }

    override fun tokenize(source: String): List<Token> {
        if (source.isEmpty()) return emptyList()
        val bytes = source.toByteArray(Charsets.UTF_8)
        val byteToChar = buildByteToCharIndex(source, bytes.size)
        val tree = parser.parseString(null, source)
        val out = mutableListOf<Token>()
        walk(tree.rootNode, source, bytes, byteToChar, out)
        out.sortBy { it.start }
        return mergeAdjacent(out)
    }

    private fun walk(
        node: TSNode,
        source: String,
        bytes: ByteArray,
        byteToChar: IntArray,
        out: MutableList<Token>,
    ) {
        // We only emit tokens for leaf named nodes — internal nodes are
        // structural. Anonymous nodes (punctuation, keywords-as-literal-text)
        // get classified via their `type` too.
        val childCount = node.childCount
        if (childCount == 0) {
            val startByte = node.startByte
            val endByte = node.endByte
            if (endByte <= startByte) return
            val charStart = byteToChar.getOrElse(startByte) { source.length }
            val charEnd = byteToChar.getOrElse(endByte) { source.length }
            if (charEnd <= charStart) return
            val type = tokenTypeMap[node.type] ?: defaultType
            out += Token(charStart, charEnd, type)
            return
        }
        for (i in 0 until childCount) walk(node.getChild(i), source, bytes, byteToChar, out)
    }

    /**
     * Build a `byteIndex → charIndex` lookup. The map is `bytes.size + 1`
     * long so `byteToChar[endByte]` works for tokens that end at EOF.
     */
    private fun buildByteToCharIndex(source: String, byteLength: Int): IntArray {
        val map = IntArray(byteLength + 1)
        var byteIdx = 0
        for (charIdx in source.indices) {
            map[byteIdx] = charIdx
            val cp = source.codePointAt(charIdx)
            // UTF-8 byte count for this code point. Surrogate-pair input is
            // already handled by indexing — the high surrogate's UTF-8 width
            // is the full pair's.
            val width = when {
                cp < 0x80 -> 1
                cp < 0x800 -> 2
                cp < 0x10000 -> 3
                else -> 4
            }
            for (j in 1 until width) {
                if (byteIdx + j <= byteLength) map[byteIdx + j] = charIdx
            }
            byteIdx += width
            if (cp >= 0x10000) {
                // Surrogate pair: the high surrogate contributed; the low
                // surrogate's char index isn't a valid token boundary, but
                // we still need an entry for it.
            }
        }
        // Fill the trailing slot.
        for (i in byteIdx..byteLength) map[i] = source.length
        return map
    }

    /**
     * Merge runs of same-type tokens that are contiguous. Tree-sitter often
     * emits adjacent named children (e.g. consecutive identifiers, punctuation
     * runs) that we want to display as a single styled span.
     */
    private fun mergeAdjacent(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val merged = mutableListOf<Token>()
        var current = tokens.first()
        for (next in tokens.drop(1)) {
            if (next.type == current.type && next.start == current.end) {
                current = Token(current.start, next.end, current.type)
            } else {
                merged += current
                current = next
            }
        }
        merged += current
        return merged
    }
}
