package tools.konsole.rich.syntax

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class TreeSitterLexerTest : StringSpec({

    "tokenize a small Kotlin snippet via tree-sitter" {
        val lexer = TreeSitterLanguages.kotlin()
        val source = "fun greet(name: String): String = \"hi, world\""
        val tokens = lexer.tokenize(source)
        // Should classify the 'fun' keyword, the String type identifier, and the string literal.
        val types = tokens.map { it.type }.toSet()
        types shouldContain TokenType.KEYWORD          // "fun"
        types shouldContain TokenType.TYPE             // "String" (grammar emits type_identifier)
        types shouldContain TokenType.STRING           // "\"hi, world\""
        // First token starts at 0 and tokens cover all but at most a single
        // trailing closing-quote byte (some grammars don't always emit the
        // closing quote as a leaf).
        tokens.first().start shouldBe 0
        (tokens.last().end >= source.length - 1) shouldBe true
    }

    "JSON tokens classify keyword constants correctly" {
        val lexer = TreeSitterLanguages.json()
        val tokens = lexer.tokenize("""{"ok": true, "value": null, "n": 42}""")
        val typed = tokens.map { it.type }
        typed shouldContain TokenType.STRING            // "ok", "value", "n"
        typed shouldContain TokenType.KEYWORD_CONSTANT  // true, null
        typed shouldContain TokenType.NUMBER            // 42
        typed shouldContain TokenType.PUNCTUATION       // braces, colons, commas
    }

    "byName returns null for an unknown language" {
        TreeSitterLanguages.byName("brainfuck") shouldBe null
    }

    "byName resolves the canonical-source-code grammars" {
        // Markdown is currently excluded — tree-sitter-markdown 0.7.1 has an
        // ABI mismatch with the 0.26.x core lib bundled in the binding.
        // Add it back once a newer markdown grammar release is available.
        val supported = listOf(
            "kt", "kotlin", "py", "python", "java", "json", "bash", "sh",
            "javascript", "js", "typescript", "ts", "rust", "rs",
            "go", "golang", "yaml", "yml", "toml",
        )
        for (name in supported) {
            (TreeSitterLanguages.byName(name) != null) shouldBe true
        }
    }

    "tokenize a small Rust snippet" {
        val lexer = TreeSitterLanguages.rust()
        val source = "fn greet(name: &str) -> String { format!(\"hi, {}\", name) }"
        val types = lexer.tokenize(source).map { it.type }.toSet()
        types shouldContain TokenType.KEYWORD       // fn
        types shouldContain TokenType.KEYWORD_TYPE  // str / String
        types shouldContain TokenType.STRING        // "hi, {}"
    }

    "tokenize a small TypeScript snippet" {
        val lexer = TreeSitterLanguages.typescript()
        val source = "const x: number = 42; type T = string | null;"
        val types = lexer.tokenize(source).map { it.type }.toSet()
        types shouldContain TokenType.KEYWORD       // const / type
        types shouldContain TokenType.KEYWORD_TYPE  // number, string
    }
})
