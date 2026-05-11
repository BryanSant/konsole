package tools.konsole.rich.syntax

import org.treesitter.TSLanguage

/**
 * Per-language tree-sitter integration: instantiate the bundled grammar,
 * build the node-type → [TokenType] map, return a configured
 * [TreeSitterLexer]. Each factory function loads its grammar's native
 * library lazily on first call (the bonede `io.github.bonede:tree-sitter-*`
 * artifacts auto-extract their `.so` / `.dylib` / `.dll` from the jar).
 *
 * The mapping tables are deliberately conservative: every entry corresponds
 * to a real node type name emitted by the grammar at hand. Unmapped node
 * types fall through to [TokenType.TEXT]. Cross-language consistency (e.g.
 * "string" → [TokenType.STRING] everywhere) is preserved where possible.
 */
public object TreeSitterLanguages {

    /** Tree-sitter–powered Kotlin lexer. */
    public fun kotlin(): TreeSitterLexer = TreeSitterLexer(
        language = loadLanguage("org.treesitter.TreeSitterKotlin"),
        tokenTypeMap = KOTLIN_TOKEN_MAP,
    )

    /** Tree-sitter–powered Java lexer. */
    public fun java(): TreeSitterLexer = TreeSitterLexer(
        language = loadLanguage("org.treesitter.TreeSitterJava"),
        tokenTypeMap = JAVA_TOKEN_MAP,
    )

    /** Tree-sitter–powered Python lexer. */
    public fun python(): TreeSitterLexer = TreeSitterLexer(
        language = loadLanguage("org.treesitter.TreeSitterPython"),
        tokenTypeMap = PYTHON_TOKEN_MAP,
    )

    /** Tree-sitter–powered JSON lexer. */
    public fun json(): TreeSitterLexer = TreeSitterLexer(
        language = loadLanguage("org.treesitter.TreeSitterJson"),
        tokenTypeMap = JSON_TOKEN_MAP,
    )

    /** Tree-sitter–powered Bash lexer. */
    public fun bash(): TreeSitterLexer = TreeSitterLexer(
        language = loadLanguage("org.treesitter.TreeSitterBash"),
        tokenTypeMap = BASH_TOKEN_MAP,
    )

    /** Tree-sitter–powered Markdown lexer. */
    public fun markdown(): TreeSitterLexer = TreeSitterLexer(
        language = loadLanguage("org.treesitter.TreeSitterMarkdown"),
        tokenTypeMap = MARKDOWN_TOKEN_MAP,
    )

    /** Returns `null` if the grammar's native lib isn't on the classpath. */
    public fun byName(name: String): TreeSitterLexer? = try {
        when (name.lowercase()) {
            "kotlin", "kt", "kts" -> kotlin()
            "java" -> java()
            "python", "py" -> python()
            "json" -> json()
            "bash", "sh", "shell" -> bash()
            "markdown", "md" -> markdown()
            else -> null
        }
    } catch (_: Throwable) {
        null
    }

    @Volatile private var coreExtracted = false

    private fun loadLanguage(className: String): TSLanguage {
        // Pre-extract the bundled native libraries to ~/.tree-sitter/lib/ to
        // dodge a bug in bonede's NativeUtils: it uses Files.move() with a
        // temp file in $TMPDIR, which fails across filesystems
        // (tmpfs / btrfs etc.). By pre-placing the files with the exact bytes
        // from the classpath resources, bonede's CRC check passes and it
        // jumps straight to System.load, skipping the broken move.
        //
        // We extract both the core tree-sitter engine (used by every grammar)
        // and the per-language grammar lib.
        synchronized(this) {
            if (!coreExtracted) {
                preExtract("tree-sitter")
                coreExtracted = true
            }
        }
        preExtract(grammarLibBase(className))
        return Class.forName(className).getDeclaredConstructor().newInstance() as TSLanguage
    }

    /**
     * Map a TreeSitter* class name to its bundled lib basename: e.g.
     * "org.treesitter.TreeSitterKotlin" → "tree-sitter-kotlin".
     */
    private fun grammarLibBase(className: String): String {
        val simple = className.substringAfterLast('.')      // "TreeSitterKotlin"
        require(simple.startsWith("TreeSitter")) { "unrecognised grammar class: $className" }
        val lang = simple.removePrefix("TreeSitter")
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), "-")      // "Markdown" stays; camel-split if needed
            .lowercase()
        return "tree-sitter-$lang"
    }

    /**
     * Pre-extract the lib named [libBase] (e.g. "tree-sitter",
     * "tree-sitter-kotlin") from the classpath to ~/.tree-sitter/lib/, matching
     * exactly what bonede's NativeUtils expects to find there. If the file
     * already exists with the right bytes, no-op so bonede's CRC check passes.
     */
    private fun preExtract(libBase: String) {
        val (arch, osTag, ext) = currentTriple() ?: return  // unsupported platform → let bonede fail naturally
        val fileName = "$arch-$osTag-$libBase.$ext"
        val resourceName = "lib/$fileName"
        val libDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".tree-sitter", "lib")
        java.nio.file.Files.createDirectories(libDir)
        val target = libDir.resolve(fileName)
        val resourceBytes = TreeSitterLanguages::class.java.classLoader
            .getResourceAsStream(resourceName)?.use { it.readBytes() } ?: return
        if (java.nio.file.Files.exists(target)) {
            val existing = java.nio.file.Files.readAllBytes(target)
            if (existing.contentEquals(resourceBytes)) return
        }
        java.nio.file.Files.write(target, resourceBytes)
    }

    private fun currentTriple(): Triple<String, String, String>? {
        val os = System.getProperty("os.name", "").lowercase()
        val arch = System.getProperty("os.arch", "").lowercase()
        val archTag = when {
            arch.contains("amd64") || arch.contains("x86_64") -> "x86_64"
            arch.contains("aarch64") -> "aarch64"
            else -> return null
        }
        val (osTag, ext) = when {
            os.contains("linux") -> "linux-gnu" to "so"
            os.contains("mac") -> "macos" to "dylib"
            os.contains("windows") -> "windows" to "dll"
            else -> return null
        }
        return Triple(archTag, osTag, ext)
    }

    // ---- Keyword tables -------------------------------------------------

    private val KOTLIN_KEYWORDS = listOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface",
        "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
        "typeof", "val", "var", "when", "while", "by", "catch", "constructor", "delegate", "dynamic",
        "field", "file", "finally", "get", "import", "init", "param", "property", "receiver", "set",
        "setparam", "where", "actual", "abstract", "annotation", "companion", "const", "crossinline",
        "data", "enum", "expect", "external", "final", "infix", "inline", "inner", "internal", "lateinit",
        "noinline", "open", "operator", "out", "override", "private", "protected", "public", "reified",
        "sealed", "suspend", "tailrec", "vararg",
    )

    private val KOTLIN_OPERATORS = listOf(
        "+", "-", "*", "/", "%", "=", "==", "!=", "===", "!==", "<", ">", "<=", ">=", "&&", "||", "!",
        "+=", "-=", "*=", "/=", "%=", "?:", "?.", "..", "->", "::", "..<",
    )
    private val KOTLIN_PUNCT = listOf("(", ")", "[", "]", "{", "}", ";", ",", ".", ":", "?", "@")

    private val JAVA_KEYWORDS = listOf(
        "abstract", "assert", "break", "case", "catch", "class", "const", "continue", "default", "do",
        "else", "enum", "extends", "final", "finally", "for", "goto", "if", "implements", "import",
        "instanceof", "interface", "native", "new", "package", "private", "protected", "public",
        "record", "return", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
        "throws", "transient", "try", "var", "volatile", "while", "yield",
    )
    private val JAVA_OPERATORS = listOf(
        "+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!",
        "+=", "-=", "*=", "/=", "%=", "&", "|", "^", "~", "<<", ">>", ">>>", "->", "::", "?",
    )
    private val PYTHON_KEYWORDS = listOf(
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif",
        "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda",
        "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield", "match", "case",
    )

    // ---- Token maps -----------------------------------------------------
    // Each map lists the node-type strings the grammar emits that we want to
    // style. Unlisted types fall through to TEXT. Anonymous tokens such as
    // keywords (e.g. "fun", "class") appear as their literal source text in
    // tree-sitter's `type`, so we map them by literal.

    private val KOTLIN_TOKEN_MAP: Map<String, TokenType> = buildMap {
        // Keywords
        for (kw in KOTLIN_KEYWORDS) put(kw, TokenType.KEYWORD)
        for (kw in listOf("Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char", "String", "Unit", "Any", "Nothing"))
            put(kw, TokenType.KEYWORD_TYPE)
        for (kw in listOf("true", "false", "null"))
            put(kw, TokenType.KEYWORD_CONSTANT)
        // Literals & comments
        put("line_comment", TokenType.COMMENT)
        put("block_comment", TokenType.COMMENT)
        put("integer_literal", TokenType.NUMBER)
        put("real_literal", TokenType.NUMBER)
        put("hex_literal", TokenType.NUMBER)
        put("bin_literal", TokenType.NUMBER)
        put("long_literal", TokenType.NUMBER)
        put("character_literal", TokenType.STRING)
        put("line_string_literal", TokenType.STRING)
        put("multi_line_string_literal", TokenType.STRING)
        put("string_content", TokenType.STRING)
        put("string_literal", TokenType.STRING)
        put("\"", TokenType.STRING)
        put("'", TokenType.STRING)
        put("interpolated_identifier", TokenType.STRING_INTERPOLATION)
        put("interpolated_expression", TokenType.STRING_INTERPOLATION)
        put("$", TokenType.STRING_INTERPOLATION)
        // Identifiers
        put("simple_identifier", TokenType.IDENTIFIER)
        put("type_identifier", TokenType.TYPE)
        put("user_type", TokenType.TYPE)
        put("class_identifier", TokenType.CLASS)
        // Operators & punctuation
        for (op in KOTLIN_OPERATORS) put(op, TokenType.OPERATOR)
        for (p in KOTLIN_PUNCT) put(p, TokenType.PUNCTUATION)
        put("annotation", TokenType.ANNOTATION)
        put("call_expression", TokenType.FUNCTION)
    }

    private val JAVA_TOKEN_MAP: Map<String, TokenType> = buildMap {
        for (kw in JAVA_KEYWORDS) put(kw, TokenType.KEYWORD)
        for (kw in listOf("boolean", "byte", "char", "double", "float", "int", "long", "short", "void"))
            put(kw, TokenType.KEYWORD_TYPE)
        for (kw in listOf("true", "false", "null")) put(kw, TokenType.KEYWORD_CONSTANT)
        put("line_comment", TokenType.COMMENT)
        put("block_comment", TokenType.COMMENT)
        put("decimal_integer_literal", TokenType.NUMBER)
        put("hex_integer_literal", TokenType.NUMBER)
        put("octal_integer_literal", TokenType.NUMBER)
        put("binary_integer_literal", TokenType.NUMBER)
        put("decimal_floating_point_literal", TokenType.NUMBER)
        put("hex_floating_point_literal", TokenType.NUMBER)
        put("string_literal", TokenType.STRING)
        put("string_fragment", TokenType.STRING)
        put("character_literal", TokenType.STRING)
        put("\"", TokenType.STRING)
        put("escape_sequence", TokenType.STRING_ESCAPE)
        put("identifier", TokenType.IDENTIFIER)
        put("type_identifier", TokenType.TYPE)
        put("marker_annotation", TokenType.ANNOTATION)
        put("annotation", TokenType.ANNOTATION)
        for (op in JAVA_OPERATORS) put(op, TokenType.OPERATOR)
        for (p in listOf("(", ")", "[", "]", "{", "}", ";", ",", ".")) put(p, TokenType.PUNCTUATION)
    }

    private val PYTHON_TOKEN_MAP: Map<String, TokenType> = buildMap {
        for (kw in PYTHON_KEYWORDS) put(kw, TokenType.KEYWORD)
        for (kw in listOf("True", "False", "None")) put(kw, TokenType.KEYWORD_CONSTANT)
        put("comment", TokenType.COMMENT)
        put("integer", TokenType.NUMBER)
        put("float", TokenType.NUMBER)
        put("string", TokenType.STRING)
        put("string_start", TokenType.STRING)
        put("string_content", TokenType.STRING)
        put("string_end", TokenType.STRING)
        put("escape_sequence", TokenType.STRING_ESCAPE)
        put("interpolation", TokenType.STRING_INTERPOLATION)
        put("identifier", TokenType.IDENTIFIER)
        put("type", TokenType.TYPE)
        put("decorator", TokenType.DECORATOR)
        for (op in listOf("+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "and", "or", "not", "in", "is"))
            put(op, TokenType.OPERATOR)
        for (p in listOf("(", ")", "[", "]", "{", "}", ":", ",", ".", ";")) put(p, TokenType.PUNCTUATION)
    }

    private val JSON_TOKEN_MAP: Map<String, TokenType> = mapOf(
        "string" to TokenType.STRING,
        "string_content" to TokenType.STRING,
        "number" to TokenType.NUMBER,
        "true" to TokenType.KEYWORD_CONSTANT,
        "false" to TokenType.KEYWORD_CONSTANT,
        "null" to TokenType.KEYWORD_CONSTANT,
        "{" to TokenType.PUNCTUATION,
        "}" to TokenType.PUNCTUATION,
        "[" to TokenType.PUNCTUATION,
        "]" to TokenType.PUNCTUATION,
        ":" to TokenType.PUNCTUATION,
        "," to TokenType.PUNCTUATION,
        "\"" to TokenType.STRING,
        "escape_sequence" to TokenType.STRING_ESCAPE,
        "comment" to TokenType.COMMENT,
    )

    private val BASH_TOKEN_MAP: Map<String, TokenType> = buildMap {
        for (kw in listOf("if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "do", "done",
            "function", "in", "return", "break", "continue", "local", "export", "readonly", "declare", "unset"))
            put(kw, TokenType.KEYWORD)
        put("comment", TokenType.COMMENT)
        put("string", TokenType.STRING)
        put("raw_string", TokenType.STRING)
        put("ansi_c_string", TokenType.STRING)
        put("string_content", TokenType.STRING)
        put("number", TokenType.NUMBER)
        put("variable_name", TokenType.IDENTIFIER)
        put("simple_variable_name", TokenType.IDENTIFIER)
        put("special_variable_name", TokenType.PROPERTY)
        put("command_substitution", TokenType.STRING_INTERPOLATION)
        put("expansion", TokenType.STRING_INTERPOLATION)
        put("$", TokenType.STRING_INTERPOLATION)
        for (op in listOf("&&", "||", "|", "&", ">", "<", ">>", "<<", "=", "==", "!=")) put(op, TokenType.OPERATOR)
        for (p in listOf("(", ")", "[", "]", "{", "}", ";", ",")) put(p, TokenType.PUNCTUATION)
    }

    private val MARKDOWN_TOKEN_MAP: Map<String, TokenType> = mapOf(
        "atx_heading_marker" to TokenType.KEYWORD,
        "setext_heading" to TokenType.KEYWORD,
        "code_fence_content" to TokenType.STRING,
        "fenced_code_block" to TokenType.STRING,
        "indented_code_block" to TokenType.STRING,
        "info_string" to TokenType.COMMENT,
        "html_block" to TokenType.HTML_TAG,
        "link_destination" to TokenType.STRING,
        "link_text" to TokenType.IDENTIFIER,
        "block_quote_marker" to TokenType.COMMENT,
        "list_marker_minus" to TokenType.OPERATOR,
        "list_marker_plus" to TokenType.OPERATOR,
        "list_marker_star" to TokenType.OPERATOR,
        "list_marker_dot" to TokenType.OPERATOR,
        "thematic_break" to TokenType.OPERATOR,
    )

}
