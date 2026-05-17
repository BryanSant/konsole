package tools.konsole.rich.traceback

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Group
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.syntax.Syntax
import tools.konsole.rich.syntax.SyntaxTheme
import tools.konsole.core.style.Color
import java.io.File

/**
 * Pretty-printed [Throwable]. Mirrors `rich.traceback.Traceback`.
 *
 * For each frame in the cause chain we render a [Panel] containing a [Syntax]-highlighted
 * source snippet (when the source file is locatable on disk) and the throw site marker.
 */
/**
 * Pretty-printed [Throwable]. Mirrors `rich.traceback.Traceback`.
 *
 * Constructor surface follows Python rich's `Traceback.__init__`:
 *
 * @param throwable the exception to render.
 * @param codeTheme syntax theme for source snippets. Konsole-equivalent of rich's `theme` param.
 * @param maxFrames cap on rendered frames (deepest first). Mirrors rich's `max_frames`.
 * @param showLocals **accepted but no-op on the JVM** — JVM stack frames don't carry locals at
 *   exception-rendering time; would require a JPDA debugger attach. Kept for API parity so code
 *   ported from Python compiles unchanged.
 * @param suppress class-name prefixes whose frames are dropped (e.g. internal coroutine machinery).
 * @param width overall render width. `null` = caller's max width.
 * @param codeWidth target width for the source snippet panel. Mirrors rich's `code_width`.
 * @param extraLines lines of context before/after each throw site. Mirrors rich's `extra_lines`.
 * @param wordWrap word-wrap long lines in code snippets. Mirrors rich's `word_wrap`.
 * @param indentGuides draw vertical guides at code indent levels. Mirrors rich's `indent_guides`.
 * @param localsMaxLength accepted for API parity with rich (used by Python locals rendering — see [showLocals]).
 * @param localsMaxString accepted for API parity with rich.
 * @param localsMaxDepth accepted for API parity with rich.
 */
public class Traceback(
    public val throwable: Throwable,
    public val codeTheme: SyntaxTheme = SyntaxTheme.MONOKAI,
    public val maxFrames: Int = 100,
    public val showLocals: Boolean = false,
    public val suppress: List<String> = emptyList(),
    public val width: Int? = 100,
    public val codeWidth: Int? = 88,
    public val extraLines: Int = 3,
    public val wordWrap: Boolean = false,
    public val indentGuides: Boolean = true,
    public val localsMaxLength: Int = 10,
    public val localsMaxString: Int = 80,
    public val localsMaxDepth: Int? = null,
) : Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> =
        renderable(console, options).render(console, options)

    override fun measure(console: Console, options: RenderOptions): Measurement =
        Measurement(0, options.maxWidth)

    private fun renderable(console: Console, options: RenderOptions): Renderable {
        val parts = mutableListOf<Renderable>()
        var current: Throwable? = throwable
        var first = true
        while (current != null) {
            if (!first) {
                parts += Markup.parse("\n[bold red]The above exception was the direct cause of the following:[/]\n")
            }
            parts += renderSingle(current)
            first = false
            current = current.cause
        }
        return Group(parts)
    }

    private fun renderSingle(t: Throwable): Renderable {
        val header = Markup.parse(":boom: [bold red]${t::class.qualifiedName ?: "Throwable"}[/]: " +
            (t.message?.let { escape(it) } ?: ""))

        val stack = t.stackTrace.toList().filter { st ->
            suppress.none { st.className.startsWith(it) }
        }
        val visible = stack.take(maxFrames)
        val omitted = (stack.size - visible.size).coerceAtLeast(0)

        val frames: List<Renderable> = visible.map { frame -> renderFrame(frame) }

        val notes = if (omitted > 0) {
            listOf(Markup.parse("[dim]… ${omitted} more frames suppressed[/]"))
        } else emptyList()

        val panel = Panel(
            renderable = Group(listOf(header) + frames + notes),
            title = Markup.parse("[red bold]Traceback[/]"),
            box = Box.HEAVY,
            borderStyle = Style(color = Color.Red),
        )
        return panel
    }

    private fun renderFrame(frame: StackTraceElement): Renderable {
        val location = "${frame.className}.${frame.methodName}"
        val file = frame.fileName ?: "<unknown>"
        val line = frame.lineNumber
        val sourceFile = locateSource(frame)
        val snippet: Renderable? = sourceFile?.let { f ->
            val lines = try { f.readLines() } catch (_: Exception) { return@let null }
            val from = (line - extraLines - 1).coerceAtLeast(0)
            val to = (line + extraLines - 1).coerceAtMost(lines.lastIndex)
            if (from > to) return@let null
            val code = lines.subList(from, to + 1).joinToString("\n")
            Syntax(
                code = code,
                language = Syntax.guessLexer(f.name) ?: "text",
                theme = codeTheme,
                lineNumbers = true,
                startLine = from + 1,
                highlightLines = setOf(line),
            )
        }
        val header = Markup.parse(
            "[dim]File[/] [magenta]${escape(file)}[/] [dim]line[/] [yellow]$line[/], " +
                "[dim]in[/] [bold]${escape(location)}[/]"
        )
        return if (snippet != null) Group(header, snippet) else header
    }

    private fun locateSource(frame: StackTraceElement): File? {
        // Best-effort: look for src/main/kotlin/<package-path>/<filename>
        val name = frame.fileName ?: return null
        val pkg = frame.className.substringBeforeLast('.', "").replace('.', '/')
        val candidates = listOf(
            File("src/main/kotlin/$pkg/$name"),
            File("src/main/java/$pkg/$name"),
            File("konsole-rich/src/main/kotlin/$pkg/$name"),
            File("krossterm/src/main/kotlin/$pkg/$name"),
            File("examples/src/main/kotlin/$pkg/$name"),
        )
        return candidates.firstOrNull { it.exists() && it.canRead() }
    }

    private fun escape(s: String): String = Markup.escape(s)

    public companion object {
        /** Install a default uncaught-exception handler that pretty-prints exceptions. */
        public fun install(console: Console = Console.system(), suppress: List<String> = emptyList()) {
            Thread.setDefaultUncaughtExceptionHandler { _, e ->
                console.print(Traceback(e, suppress = suppress))
            }
        }
    }
}

/** Convenience: render a [Throwable] via [Console.print]. */
public fun Console.printException(
    t: Throwable,
    showLocals: Boolean = false,
    suppress: List<String> = emptyList(),
) {
    print(Traceback(t, showLocals = showLocals, suppress = suppress))
}
