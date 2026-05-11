package tools.konsole.rich.syntax

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class SyntaxTest : StringSpec({

    "Kotlin lexer tokenizes keywords and strings" {
        val tokens = (Lexer.forLanguage("kotlin"))!!.tokenize("""val x = "hello"""")
        val types = tokens.map { it.type }
        // Should contain at least one KEYWORD ('val'), IDENTIFIER ('x'), and STRING
        (TokenType.KEYWORD in types) shouldBe true
        (TokenType.STRING in types) shouldBe true
    }

    "Python lexer tokenizes keywords and docstrings" {
        val src = "\"\"\"docstring\"\"\"\ndef foo():\n    return None\n"
        val tokens = (Lexer.forLanguage("python"))!!.tokenize(src)
        val types = tokens.map { it.type }
        (TokenType.KEYWORD in types) shouldBe true
        (TokenType.NULL in types) shouldBe true
        (TokenType.DOCSTRING in types) shouldBe true
    }

    "JSON lexer tokenizes basic literals" {
        val tokens = (Lexer.forLanguage("json"))!!.tokenize("""{"k": 42, "v": true, "n": null}""")
        val types = tokens.map { it.type }
        (TokenType.STRING in types) shouldBe true
        (TokenType.NUMBER in types) shouldBe true
        (TokenType.BOOL in types) shouldBe true
        (TokenType.NULL in types) shouldBe true
    }

    "Syntax renders code to console" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(Syntax("val x = 1", "kotlin", lineNumbers = true, theme = SyntaxTheme.ANSI_DARK))
        val out = sw.toString()
        out shouldContain "val"
        out shouldContain "x"
        out shouldContain "1"
        // Line number should be present
        out shouldContain "1  "
    }

    "guessLexer maps file extensions" {
        Syntax.guessLexer("foo.kt") shouldBe "kotlin"
        Syntax.guessLexer("foo.py") shouldBe "python"
        Syntax.guessLexer("foo.json") shouldBe "json"
        Syntax.guessLexer("foo.unknown") shouldBe null
    }
})
