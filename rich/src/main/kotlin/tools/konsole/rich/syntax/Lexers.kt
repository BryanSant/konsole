package tools.konsole.rich.syntax

import tools.konsole.rich.syntax.RegexLexer.Rule

/**
 * Per-language lexers. These are deliberately simple regex-based tokenizers — they cover
 * the common cases for terminal display, not full grammar correctness.
 */

internal val KotlinLexer: Lexer = RegexLexer.of(
    Rule(Regex("""//[^\n]*"""), TokenType.COMMENT),
    Rule(Regex("""/\*[\s\S]*?\*/"""), TokenType.COMMENT),
    Rule(Regex("""\"\"\"[\s\S]*?\"\"\""""), TokenType.STRING),
    Rule(Regex("""\"(?:\\.|[^"\\])*\""""), TokenType.STRING),
    Rule(Regex("""'(?:\\.|[^'\\])*'"""), TokenType.STRING),
    Rule(Regex("""@\w+"""), TokenType.ANNOTATION),
    Rule(Regex("""\b(true|false)\b"""), TokenType.BOOL),
    Rule(Regex("""\bnull\b"""), TokenType.NULL),
    Rule(
        Regex(
            "\\b(fun|val|var|class|object|interface|abstract|open|sealed|data|enum|annotation|" +
                "if|else|when|for|while|do|return|continue|break|try|catch|finally|throw|" +
                "package|import|public|private|protected|internal|companion|override|operator|" +
                "infix|inline|suspend|tailrec|external|const|init|this|super|in|out|by|where|reified|crossinline|noinline|" +
                "as|is|typealias|lateinit|vararg|get|set|field|delegate|param|setparam|file|receiver|property|setter|getter|expect|actual)\\b"
        ),
        TokenType.KEYWORD,
    ),
    Rule(Regex("""\b(Int|Long|Float|Double|Boolean|String|Char|Byte|Short|Unit|Nothing|Any|Array|List|Map|Set|Pair|Triple)\b"""), TokenType.KEYWORD_TYPE),
    Rule(Regex("""\b\d[\d_]*\.[\d_]+([eE][+\-]?\d+)?[fF]?\b|\b\d[\d_]*[eE][+\-]?\d+[fF]?\b|\b\d[\d_]*[lL]?\b|\b0[xX][0-9a-fA-F_]+[lL]?\b|\b0[bB][01_]+[lL]?\b"""), TokenType.NUMBER),
    Rule(Regex("""\b[A-Z][\w$]*"""), TokenType.TYPE),
    Rule(Regex("""\b[a-z_][\w$]*(?=\s*\()"""), TokenType.FUNCTION),
    Rule(Regex("""\b[a-z_][\w$]*"""), TokenType.IDENTIFIER),
    Rule(Regex("""[+\-*/%=<>!&|^~?:]+"""), TokenType.OPERATOR),
    Rule(Regex("""[(){}\[\];,.@]"""), TokenType.PUNCTUATION),
)

internal val JavaLexer: Lexer = RegexLexer.of(
    Rule(Regex("""//[^\n]*"""), TokenType.COMMENT),
    Rule(Regex("""/\*[\s\S]*?\*/"""), TokenType.COMMENT),
    Rule(Regex("""\"(?:\\.|[^"\\])*\""""), TokenType.STRING),
    Rule(Regex("""'(?:\\.|[^'\\])*'"""), TokenType.STRING),
    Rule(Regex("""@\w+(\s*\([^)]*\))?"""), TokenType.ANNOTATION),
    Rule(Regex("""\b(true|false)\b"""), TokenType.BOOL),
    Rule(Regex("""\bnull\b"""), TokenType.NULL),
    Rule(
        Regex(
            "\\b(abstract|assert|break|case|catch|class|continue|default|do|else|enum|extends|" +
                "final|finally|for|goto|if|implements|import|instanceof|interface|module|native|" +
                "new|package|private|protected|public|record|return|sealed|static|strictfp|super|" +
                "switch|synchronized|this|throw|throws|transient|try|var|volatile|while|yield)\\b"
        ),
        TokenType.KEYWORD,
    ),
    Rule(Regex("""\b(boolean|byte|char|double|float|int|long|short|void|String|Object|Integer|Boolean|Long|Double|Float)\b"""), TokenType.KEYWORD_TYPE),
    Rule(Regex("""\b\d[\d_]*\.[\d_]+([eE][+\-]?\d+)?[fFdD]?\b|\b\d[\d_]*[eE][+\-]?\d+[fFdD]?\b|\b\d[\d_]*[lL]?\b|\b0[xX][0-9a-fA-F_]+[lL]?\b"""), TokenType.NUMBER),
    Rule(Regex("""\b[A-Z][\w$]*"""), TokenType.TYPE),
    Rule(Regex("""\b[a-z_][\w$]*(?=\s*\()"""), TokenType.FUNCTION),
    Rule(Regex("""\b[a-z_][\w$]*"""), TokenType.IDENTIFIER),
    Rule(Regex("""[+\-*/%=<>!&|^~?:]+"""), TokenType.OPERATOR),
    Rule(Regex("""[(){}\[\];,.]"""), TokenType.PUNCTUATION),
)

internal val PythonLexer: Lexer = RegexLexer.of(
    Rule(Regex("""#[^\n]*"""), TokenType.COMMENT),
    Rule(Regex("""[bBrRuUfF]{0,2}\"\"\"[\s\S]*?\"\"\""""), TokenType.DOCSTRING),
    Rule(Regex("""[bBrRuUfF]{0,2}'''[\s\S]*?'''"""), TokenType.DOCSTRING),
    Rule(Regex("""[bBrRuUfF]{0,2}\"(?:\\.|[^"\\])*\""""), TokenType.STRING),
    Rule(Regex("""[bBrRuUfF]{0,2}'(?:\\.|[^'\\])*'"""), TokenType.STRING),
    Rule(Regex("""@\w+(\.\w+)*"""), TokenType.DECORATOR),
    Rule(Regex("""\b(True|False)\b"""), TokenType.BOOL),
    Rule(Regex("""\bNone\b"""), TokenType.NULL),
    Rule(
        Regex(
            "\\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|" +
                "else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|" +
                "pass|raise|return|try|while|with|yield|match|case)\\b"
        ),
        TokenType.KEYWORD,
    ),
    Rule(Regex("""\b(int|float|str|bool|list|dict|set|tuple|bytes|bytearray|object|complex|frozenset|range|type)\b"""), TokenType.KEYWORD_TYPE),
    Rule(Regex("""\b\d+\.\d+([eE][+\-]?\d+)?[jJ]?\b|\b\d+[eE][+\-]?\d+[jJ]?\b|\b\d+[jJ]?\b|\b0[xX][0-9a-fA-F]+\b|\b0[oO][0-7]+\b|\b0[bB][01]+\b"""), TokenType.NUMBER),
    Rule(Regex("""\b[A-Z][\w]*"""), TokenType.TYPE),
    Rule(Regex("""\bdef\s+(\w+)"""), TokenType.FUNCTION),
    Rule(Regex("""\b[a-z_][\w]*(?=\s*\()"""), TokenType.FUNCTION),
    Rule(Regex("""\b[a-z_][\w]*"""), TokenType.IDENTIFIER),
    Rule(Regex("""[+\-*/%=<>!&|^~@]+"""), TokenType.OPERATOR),
    Rule(Regex("""[(){}\[\]:;,.]"""), TokenType.PUNCTUATION),
)

internal val JsonLexer: Lexer = RegexLexer.of(
    Rule(Regex("""\"(?:\\.|[^"\\])*\""""), TokenType.STRING),
    Rule(Regex("""\b(true|false)\b"""), TokenType.BOOL),
    Rule(Regex("""\bnull\b"""), TokenType.NULL),
    Rule(Regex("""-?\b\d+\.\d+([eE][+\-]?\d+)?\b|-?\b\d+([eE][+\-]?\d+)?\b"""), TokenType.NUMBER),
    Rule(Regex("""[{}\[\]:,]"""), TokenType.PUNCTUATION),
)

internal val BashLexer: Lexer = RegexLexer.of(
    Rule(Regex("""#[^\n]*"""), TokenType.COMMENT),
    Rule(Regex("""\"(?:\\.|[^"\\])*\""""), TokenType.STRING),
    Rule(Regex("""'[^']*'"""), TokenType.STRING),
    Rule(Regex("""\$\{[^}]+\}"""), TokenType.STRING_INTERPOLATION),
    Rule(Regex("""\$\w+"""), TokenType.STRING_INTERPOLATION),
    Rule(
        Regex(
            "\\b(if|then|else|elif|fi|for|while|do|done|case|esac|in|function|return|" +
                "break|continue|exit|export|local|readonly|declare|set|unset|source|alias|" +
                "shift|trap|wait|test|true|false)\\b"
        ),
        TokenType.KEYWORD,
    ),
    Rule(Regex("""\b\d+\b"""), TokenType.NUMBER),
    Rule(Regex("""\b[a-zA-Z_][\w-]*(?==)"""), TokenType.PROPERTY),
    Rule(Regex("""[|&;<>(){}\[\]=]"""), TokenType.OPERATOR),
)

internal val SqlLexer: Lexer = RegexLexer.of(
    Rule(Regex("""--[^\n]*"""), TokenType.COMMENT),
    Rule(Regex("""/\*[\s\S]*?\*/"""), TokenType.COMMENT),
    Rule(Regex("""'(?:''|[^'])*'"""), TokenType.STRING),
    Rule(Regex("""\"[^"]*\""""), TokenType.STRING),
    Rule(
        Regex(
            "(?i)\\b(SELECT|FROM|WHERE|JOIN|INNER|OUTER|LEFT|RIGHT|FULL|CROSS|ON|" +
                "GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT|OFFSET|UNION|INTERSECT|EXCEPT|" +
                "INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|DROP|ALTER|TABLE|VIEW|INDEX|" +
                "DATABASE|SCHEMA|GRANT|REVOKE|COMMIT|ROLLBACK|BEGIN|END|TRANSACTION|" +
                "AS|AND|OR|NOT|IN|EXISTS|LIKE|ILIKE|BETWEEN|IS|NULL|TRUE|FALSE|" +
                "DISTINCT|ALL|ANY|SOME|CASE|WHEN|THEN|ELSE|IF|RETURNING|WITH|RECURSIVE)\\b"
        ),
        TokenType.SQL_KEYWORD,
    ),
    Rule(Regex("""\b\d+\.\d+\b|\b\d+\b"""), TokenType.NUMBER),
    Rule(Regex("""\b[A-Z_][A-Z0-9_]*\b"""), TokenType.TYPE),
    Rule(Regex("""\b[a-zA-Z_][\w]*\b"""), TokenType.IDENTIFIER),
    Rule(Regex("""[(),;.*=<>!+\-/]"""), TokenType.OPERATOR),
)

internal val YamlLexer: Lexer = RegexLexer.of(
    Rule(Regex("""#[^\n]*"""), TokenType.COMMENT),
    Rule(Regex("""---|\.\.\."""), TokenType.PUNCTUATION),
    Rule(Regex("""\"(?:\\.|[^"\\])*\""""), TokenType.STRING),
    Rule(Regex("""'(?:''|[^'])*'"""), TokenType.STRING),
    Rule(Regex("""(?m)^[ \t]*[a-zA-Z_][\w.\- ]*(?=:)"""), TokenType.YAML_KEY),
    Rule(Regex("""\b(true|false|yes|no|on|off|null|~)\b"""), TokenType.BOOL),
    Rule(Regex("""-?\b\d+\.\d+([eE][+\-]?\d+)?\b|-?\b\d+\b"""), TokenType.NUMBER),
    Rule(Regex("""[\[\]{},:|>&*!?-]"""), TokenType.PUNCTUATION),
)

internal val XmlLexer: Lexer = RegexLexer.of(
    Rule(Regex("""<!--[\s\S]*?-->"""), TokenType.COMMENT),
    Rule(Regex("""<!\[CDATA\[[\s\S]*?\]\]>"""), TokenType.STRING),
    Rule(Regex("""\"(?:\\.|[^"\\])*\""""), TokenType.STRING),
    Rule(Regex("""'(?:\\.|[^'\\])*'"""), TokenType.STRING),
    Rule(Regex("""</?\s*[a-zA-Z][\w:.-]*"""), TokenType.HTML_TAG),
    Rule(Regex("""\b[a-zA-Z][\w:.-]*(?==)"""), TokenType.HTML_ATTR),
    Rule(Regex("""[<>=/]"""), TokenType.PUNCTUATION),
)

internal val HtmlLexer: Lexer = XmlLexer

internal val MarkdownLexer: Lexer = RegexLexer.of(
    Rule(Regex("""(?m)^#{1,6}\s+[^\n]+"""), TokenType.KEYWORD), // headings
    Rule(Regex("""(?m)^\s{0,3}>[^\n]*"""), TokenType.COMMENT), // blockquotes
    Rule(Regex("""```[\s\S]*?```"""), TokenType.STRING), // fenced code
    Rule(Regex("""`[^`\n]+`"""), TokenType.STRING), // inline code
    Rule(Regex("""\*\*[^*\n]+\*\*"""), TokenType.KEYWORD_TYPE), // bold
    Rule(Regex("""\*[^*\n]+\*"""), TokenType.KEYWORD_CONSTANT), // italic
    Rule(Regex("""\[[^]]+]\([^)]+\)"""), TokenType.FUNCTION), // links
    Rule(Regex("""(?m)^\s*[-*+]\s"""), TokenType.PUNCTUATION),
    Rule(Regex("""(?m)^\s*\d+\.\s"""), TokenType.PUNCTUATION),
)
