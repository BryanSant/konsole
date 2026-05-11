package tools.konsole.textual.css

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TokenizerTest : StringSpec({

    fun toks(src: String): List<Token> = Tokenizer(src).tokenize()
        .filter { it.type !in setOf(TokenType.Whitespace, TokenType.Newline) }
        .toList()

    "tokenize a simple rule" {
        val out = toks("Widget { color: red; }")
        out.map { it.type } shouldBe listOf(
            TokenType.Ident,         // Widget
            TokenType.LeftBrace,
            TokenType.Ident,         // color
            TokenType.Colon,
            TokenType.Ident,         // red
            TokenType.Semicolon,
            TokenType.RightBrace,
            TokenType.Eof,
        )
    }

    "tokens carry source positions" {
        val src = """
            |.foo {
            |    width: 50%;
            |}
        """.trimMargin()
        val out = toks(src)
        // First non-ws token is "."
        out.first().type shouldBe TokenType.Dot
        out.first().line shouldBe 1
        // "width" is on line 2
        out.first { it.type == TokenType.Ident && it.value == "width" }.line shouldBe 2
    }

    "hex colors detected vs class selectors" {
        toks("#ff00aa").first().type shouldBe TokenType.Hex
        toks("#foo").first().type shouldBe TokenType.Hash
    }

    "fr unit recognised on numbers" {
        val out = toks("1fr 2fr")
        out.filter { it.type == TokenType.Fr }.map { it.value } shouldBe listOf("1fr", "2fr")
    }

    "percent treated as separate token after a number" {
        val out = toks("50%")
        out.map { it.type } shouldBe listOf(TokenType.Number, TokenType.Percent, TokenType.Eof)
    }

    "block comments preserved as Comment token" {
        val out = Tokenizer("/* hi */ a").tokenize()
            .filter { it.type !in setOf(TokenType.Whitespace, TokenType.Newline) }
            .toList()
        out.first().type shouldBe TokenType.Comment
    }
})
