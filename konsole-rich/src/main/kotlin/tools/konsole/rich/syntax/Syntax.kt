package tools.konsole.rich.syntax

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Syntax-highlighted source code. Mirrors `rich.syntax.Syntax`.
 *
 * Provide [code] and a [language] (or use [fromPath] to detect via file extension). Common
 * options: [theme], [lineNumbers], [wordWrap], [indentGuides], [startLine], [highlightLines].
 */
public class Syntax(
    public val code: String,
    public val language: String,
    public val theme: SyntaxTheme = SyntaxTheme.MONOKAI,
    public val lineNumbers: Boolean = false,
    public val wordWrap: Boolean = false,
    public val indentGuides: Boolean = false,
    public val startLine: Int = 1,
    public val highlightLines: Set<Int> = emptySet(),
    public val backgroundColor: tools.konsole.core.style.Color? = null,
    public val padding: Int = 0,
) : Measurable {

    private val lexer: Lexer = Lexer.forLanguage(language) ?: Lexer { source ->
        listOf(Token(0, source.length, TokenType.TEXT))
    }
    private val tokens: List<Token> = lexer.tokenize(code)

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val lines = code.lines()
        val totalLines = lines.size
        val numWidth = if (lineNumbers) (startLine + totalLines - 1).toString().length else 0
        val gutter = if (lineNumbers) numWidth + 2 else 0
        val codeWidth = (options.maxWidth - gutter).coerceAtLeast(1)
        val bg = backgroundColor ?: theme.background?.bgcolor
        val baseLineStyle = if (bg != null) Style(bgcolor = bg) else Style.NULL

        // Build a map from char index → token for quick lookup.
        val tokenAt = IntArray(code.length + 1) { -1 }
        for ((idx, tok) in tokens.withIndex()) {
            for (i in tok.start until tok.end.coerceAtMost(code.length)) tokenAt[i] = idx
        }

        var charCursor = 0
        for ((lineIdx, line) in lines.withIndex()) {
            val lineNumber = startLine + lineIdx
            val isHighlighted = lineNumber in highlightLines
            val lineBg: tools.konsole.core.style.Color? = if (isHighlighted && theme.highlight != null) theme.highlight.bgcolor else bg

            if (lineIdx > 0) yield(Segment.LINE)

            if (lineNumbers) {
                val numStr = lineNumber.toString().padStart(numWidth)
                yield(Segment(numStr, theme[TokenType.LINE_NUMBER]))
                yield(Segment("  ", theme[TokenType.LINE_NUMBER]))
            }

            val lineStart = charCursor
            val lineEnd = lineStart + line.length
            // Walk this line, emitting segments per token boundary.
            var i = lineStart
            while (i < lineEnd) {
                val tIdx = tokenAt[i]
                if (tIdx < 0) {
                    yield(Segment(code.substring(i, i + 1), if (lineBg != null) Style(bgcolor = lineBg) else null))
                    i += 1
                } else {
                    val tok = tokens[tIdx]
                    val end = min(tok.end, lineEnd)
                    val text = code.substring(i, end)
                    val style = theme[tok.type].let { st ->
                        if (lineBg != null && st.bgcolor == null) st.copy(bgcolor = lineBg) else st
                    }
                    yield(Segment(text, if (style.isNull) null else style))
                    i = end
                }
            }

            charCursor = lineEnd + 1 // +1 for the '\n'
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val longest = code.lines().maxOfOrNull { it.length } ?: 0
        val numWidth = if (lineNumbers) (startLine + code.lines().size - 1).toString().length + 2 else 0
        val total = (longest + numWidth).coerceAtMost(options.maxWidth)
        return Measurement(total, total)
    }

    public companion object {
        /** Read a file and pick a lexer from its extension. */
        public fun fromPath(
            path: String,
            theme: SyntaxTheme = SyntaxTheme.MONOKAI,
            lineNumbers: Boolean = true,
            startLine: Int = 1,
            highlightLines: Set<Int> = emptySet(),
        ): Syntax {
            val file = File(path)
            val code = file.readText()
            val ext = file.extension.lowercase()
            val language = ext.ifEmpty { "text" }
            return Syntax(
                code = code,
                language = language,
                theme = theme,
                lineNumbers = lineNumbers,
                startLine = startLine,
                highlightLines = highlightLines,
            )
        }

        /** Guess a language from a filename extension (returns null if unknown). */
        public fun guessLexer(filename: String): String? {
            val ext = filename.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "kt", "kts" -> "kotlin"
                "java" -> "java"
                "py" -> "python"
                "json" -> "json"
                "sh", "bash" -> "bash"
                "sql" -> "sql"
                "yml", "yaml" -> "yaml"
                "xml" -> "xml"
                "html", "htm" -> "html"
                "md", "markdown" -> "markdown"
                else -> null
            }
        }
    }
}
