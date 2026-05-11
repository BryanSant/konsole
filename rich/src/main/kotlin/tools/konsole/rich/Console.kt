package tools.konsole.rich

import tools.konsole.core.ColorSystem

import tools.konsole.core.Terminal
import java.io.StringWriter
import java.io.Writer
import kotlin.reflect.KClass

/**
 * The rich-faithful entry point. Wraps a konsole [Terminal] (or any [Writer]) and provides
 * the high-level rendering API: markup, themes, tables, panels, progress, and friends.
 *
 * Construction:
 *   - [system] — wire to the system TTY via [Terminal.system].
 *   - [string] — render to an in-memory string (test/export).
 *   - constructor — full control over every parameter.
 *
 * Mutability: [theme], [markup], [emoji], [highlight], [softWrap], [record] are mutable.
 * A [Console] is **not threadsafe**; each thread should use its own.
 */
public class Console public constructor(
    public val terminal: Terminal? = null,
    public val writer: Writer = terminal?.out ?: System.out.writer(),
    public val width: Int = detectWidth(terminal),
    public val height: Int = detectHeight(terminal),
    public val colorSystem: ColorSystem = ColorSystem.detect(terminal),
    public var theme: Theme = Theme.DEFAULT,
    public var markup: Boolean = true,
    public var emoji: Boolean = true,
    public var highlight: Boolean = true,
    public var softWrap: Boolean = false,
    public var record: Boolean = false,
    public var noColor: Boolean = System.getenv("NO_COLOR")?.isNotEmpty() == true,
) : AutoCloseable {

    internal val recordBuffer: MutableList<Segment> = mutableListOf()
    internal val adapters: MutableMap<KClass<*>, ToRenderable<Any?>> = mutableMapOf()

    /** Default render options derived from console state. */
    public fun defaultRenderOptions(): RenderOptions = RenderOptions(
        maxWidth = width,
        highlight = highlight,
        markup = markup,
    )

    /**
     * Register a custom adapter so `console.print(value)` knows how to convert [T] to a [Renderable].
     * Adapters are looked up by exact class match.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> registerAdapter(type: KClass<T>, adapter: ToRenderable<T>) {
        adapters[type] = adapter as ToRenderable<Any?>
    }

    public inline fun <reified T : Any> registerAdapter(adapter: ToRenderable<T>): Unit =
        registerAdapter(T::class, adapter)

    /**
     * Convert an arbitrary value to a [Renderable].
     *
     * Resolution order: null → empty Text; Renderable → as-is; registered adapter → adapter result;
     * String → markup (if [markup]) or plain Text; everything else → `Text(value.toString())`.
     */
    public fun normalize(value: Any?): Renderable {
        if (value == null) return Text("")
        if (value is Renderable) return value
        val adapter = adapters[value::class]
        if (adapter != null) return adapter.toRenderable(value)
        if (value is String) {
            return if (markup) Text.fromMarkup(value, theme) else Text(value)
        }
        if (value is tools.konsole.core.style.StyledContent<*>) {
            // Preserve konsole-core's already-rendered SGR output.
            return RawAnsi(value.toString())
        }
        if (value is Throwable) {
            return tools.konsole.rich.traceback.Traceback(value)
        }
        return Text(value.toString())
    }

    /**
     * Pythonic print-statement replacement.
     */
    public fun print(
        vararg objects: Any?,
        sep: String = " ",
        end: String = "\n",
        style: Style? = null,
        justify: tools.konsole.rich.text.Justify? = null,
        overflow: tools.konsole.rich.text.Overflow? = null,
        noWrap: Boolean? = null,
        markup: Boolean? = null,
        highlight: Boolean? = null,
        width: Int? = null,
        crop: Boolean = true,
        emoji: Boolean? = null,
    ) {
        val markupOn = markup ?: this.markup
        val parts = objects.mapIndexed { i, obj ->
            val r = if (obj is String && markupOn) Text.fromMarkup(obj, theme) else normalize(obj)
            r
        }
        val joined: Renderable = if (parts.size == 1 && sep == " ") {
            parts.single()
        } else {
            Text().also { combined ->
                parts.forEachIndexed { i, part ->
                    if (i > 0) combined.append(sep, null)
                    when (part) {
                        is Text -> combined.append(part)
                        else -> {
                            // Materialize into the combined Text via render → segments → text/style.
                            for (seg in part.render(this, defaultRenderOptions())) {
                                if (seg.text.isNotEmpty()) combined.append(seg.text, seg.style)
                            }
                        }
                    }
                }
            }
        }
        val opts = defaultRenderOptions().update(
            maxWidth = width,
            justify = justify,
            overflow = overflow,
            noWrap = noWrap,
        )
        renderTo(joined, opts, terminalEnd = end, baseStyle = style, crop = crop)
    }

    /**
     * Render a [Renderable] with explicit [PrintOptions].
     */
    public fun print(renderable: Renderable, options: PrintOptions = PrintOptions()) {
        val opts = defaultRenderOptions().update(
            maxWidth = options.width,
            justify = options.justify,
            overflow = options.overflow,
            noWrap = options.noWrap,
            height = options.height,
        )
        renderTo(renderable, opts, terminalEnd = options.end, baseStyle = options.style, crop = options.crop)
    }

    /** Render a horizontal divider with optional centered title. */
    public fun rule(
        title: String? = null,
        style: Style = theme["rule.line"] ?: Style.NULL,
        char: Char = '─',
    ) {
        val rule = tools.konsole.rich.layout.Rule(title, style, char)
        print(rule)
    }

    /** Print with a `[INFO HH:mm:ss]` prefix; level can be customised via [theme]. */
    public fun log(vararg objects: Any?, level: LogLevel = LogLevel.INFO) {
        val ts = java.time.LocalTime.now().withNano(0).toString()
        val prefix = Text()
            .append("[", theme["log.time"] ?: Style.NULL)
            .append(ts, theme["log.time"] ?: Style.NULL)
            .append("] ", theme["log.time"] ?: Style.NULL)
            .append(level.name, theme["logging.level.${level.name.lowercase()}"] ?: Style(bold = true))
            .append(" ", null)
        val combined = prefix
        val markupOn = markup
        for ((i, obj) in objects.withIndex()) {
            if (i > 0) combined.append(" ", null)
            val r = if (obj is String && markupOn) Text.fromMarkup(obj, theme) else normalize(obj)
            if (r is Text) combined.append(r) else for (seg in r.render(this, defaultRenderOptions())) {
                if (seg.text.isNotEmpty()) combined.append(seg.text, seg.style)
            }
        }
        print(combined)
    }

    /** Capture all printed output in an in-memory buffer. */
    public inline fun <T> capture(block: (Console) -> T): CaptureResult<T> {
        val sw = StringWriter()
        val captured = Console(terminal = null, writer = sw, width = width, height = height, colorSystem = colorSystem)
        captured.theme = theme
        val result = block(captured)
        return CaptureResult(result, sw.toString())
    }

    /** Run [block] inside the alternate screen buffer. Requires a real [terminal]. */
    public inline fun <T> screen(block: () -> T): T {
        val t = terminal ?: error("Console.screen() requires a real Terminal; this Console has none.")
        return t.alternateScreen(block)
    }

    /** Run [block] with [theme] swapped in; restores the previous theme on exit. */
    public inline fun <T> useTheme(theme: Theme, block: () -> T): T {
        val prev = this.theme
        this.theme = theme
        return try {
            block()
        } finally {
            this.theme = prev
        }
    }

    /** Export everything captured while [record] was true as plain text (no SGR). */
    public fun exportText(): String = tools.konsole.rich.export.Export.text(recordBuffer)

    /** Export the captured buffer as a self-contained HTML document. */
    public fun exportHtml(title: String = "konsole-rich"): String =
        tools.konsole.rich.export.Export.html(recordBuffer, title = title)

    /** Export the captured buffer as an SVG terminal screenshot. */
    public fun exportSvg(title: String = "konsole-rich"): String =
        tools.konsole.rich.export.Export.svg(recordBuffer, title = title)

    /** Save plain-text export to [path]. */
    public fun saveText(path: String) {
        java.io.File(path).writeText(exportText())
    }

    /** Save HTML export to [path]. */
    public fun saveHtml(path: String, title: String = "konsole-rich") {
        java.io.File(path).writeText(exportHtml(title))
    }

    /** Save SVG export to [path]. */
    public fun saveSvg(path: String, title: String = "konsole-rich") {
        java.io.File(path).writeText(exportSvg(title))
    }

    /** Reset the record buffer. */
    public fun clearRecord(): Unit = recordBuffer.clear()

    override fun close() {
        terminal?.close()
    }

    /**
     * Run the full pipeline (resolve → split → emit) and write commands to [writer].
     * Internal — used by [print] and [rule]; tests can call directly.
     */
    internal fun renderTo(
        renderable: Renderable,
        options: RenderOptions,
        terminalEnd: String,
        baseStyle: Style?,
        crop: Boolean,
    ) {
        Pipeline.run(this, renderable, options, terminalEnd, baseStyle, crop)
    }

    public companion object {
        /** A Console wired to the system TTY. */
        public fun system(): Console = Console(terminal = Terminal.system())

        /** A Console that renders to an in-memory string. Useful for tests and exporters. */
        public fun string(width: Int = 80, colorSystem: ColorSystem = ColorSystem.TrueColor): Console =
            Console(terminal = null, writer = StringWriter(), width = width, colorSystem = colorSystem)

        /** A test-friendly Console with a known width and color system. */
        public fun test(
            width: Int = 80,
            height: Int = 24,
            colorSystem: ColorSystem = ColorSystem.TrueColor,
        ): Console = Console(
            terminal = null,
            writer = StringWriter(),
            width = width,
            height = height,
            colorSystem = colorSystem,
        )
    }
}

/** Options for [Console.print] when you have a single [Renderable] to print. */
public data class PrintOptions(
    public val style: Style? = null,
    public val justify: tools.konsole.rich.text.Justify? = null,
    public val overflow: tools.konsole.rich.text.Overflow? = null,
    public val noWrap: Boolean? = null,
    public val width: Int? = null,
    public val height: Int? = null,
    public val end: String = "\n",
    public val crop: Boolean = true,
)

public data class CaptureResult<T>(public val value: T, public val output: String)

public enum class LogLevel { DEBUG, INFO, WARNING, ERROR, CRITICAL }

internal fun detectWidth(terminal: Terminal?): Int {
    System.getenv("COLUMNS")?.toIntOrNull()?.let { return it }
    return terminal?.size?.columns ?: 80
}

internal fun detectHeight(terminal: Terminal?): Int {
    System.getenv("LINES")?.toIntOrNull()?.let { return it }
    return terminal?.size?.rows ?: 24
}
