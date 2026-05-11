package tools.konsole.rich.logging

import tools.konsole.rich.Console
import tools.konsole.rich.LogLevel
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.layout.Group
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.traceback.Traceback
import tools.konsole.core.style.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

/**
 * A [java.util.logging.Handler] that formats records using a konsole-rich [Console].
 *
 * Output looks like rich's `RichHandler`: `[time] LEVEL message  (file:line)`.
 * Throwables in records are rendered via [Traceback].
 *
 * Usage:
 *   ```
 *   val rootLogger = java.util.logging.Logger.getLogger("")
 *   rootLogger.handlers.forEach { rootLogger.removeHandler(it) }
 *   rootLogger.addHandler(RichHandler(console))
 *   ```
 */
public class KonsoleLogHandler(
    public val console: Console,
    public val showTime: Boolean = true,
    public val showLevel: Boolean = true,
    public val showPath: Boolean = true,
    public val markup: Boolean = false,
    public val richTracebacks: Boolean = true,
    public val tracebacksShowLocals: Boolean = false,
    public val tracebacksWordWrap: Boolean = false,
    public val tracebacksSuppress: List<String> = emptyList(),
    public val tracebacksExtraLines: Int = 3,
    public val localsMaxLength: Int = 10,
    public val localsMaxString: Int = 80,
    public val localsMaxDepth: Int? = null,
    public val omitRepeatedTimes: Boolean = true,
    public val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss"),
) : Handler() {

    private var lastTime: String = ""

    override fun publish(record: LogRecord) {
        if (!isLoggable(record)) return
        val message = record.message ?: ""
        val rendered = if (markup) Markup.parse(message) else Text(message)

        val parts = mutableListOf<tools.konsole.rich.Renderable>()
        val header = Text()

        if (showTime) {
            val formatted = LocalTime.now().format(timeFormat)
            val display = if (omitRepeatedTimes && formatted == lastTime) " ".repeat(formatted.length) else formatted
            lastTime = formatted
            header.append("[", Style(color = Color.DarkGrey))
            header.append(display, console.theme["log.time"] ?: Style(color = Color.Cyan, dim = true))
            header.append("] ", Style(color = Color.DarkGrey))
        }

        if (showLevel) {
            val (lvl, style) = levelStyle(record.level)
            header.append(lvl.padEnd(8), style)
        }

        // Prepend the header text.
        parts += Group(header, rendered)

        // Trailing path.
        if (showPath) {
            val src = record.sourceClassName ?: record.loggerName ?: ""
            val mt = record.sourceMethodName?.let { ".$it()" } ?: ""
            if (src.isNotEmpty()) {
                parts += Text("    " + src + mt, style = console.theme["log.path"] ?: Style(dim = true))
            }
        }

        console.print(Group(parts))

        // Throwable: render via Traceback.
        record.thrown?.let { thrown ->
            if (richTracebacks) {
                console.print(
                    Traceback(
                        thrown,
                        showLocals = tracebacksShowLocals,
                        suppress = tracebacksSuppress,
                        wordWrap = tracebacksWordWrap,
                        extraLines = tracebacksExtraLines,
                        localsMaxLength = localsMaxLength,
                        localsMaxString = localsMaxString,
                        localsMaxDepth = localsMaxDepth,
                    )
                )
            } else {
                console.print(Text(thrown.stackTraceToString(), style = Style(color = Color.Red)))
            }
        }
    }

    override fun flush() {
        console.writer.flush()
    }

    override fun close() { /* don't close the console — caller owns it */ }

    private fun levelStyle(level: Level): Pair<String, Style> = when (level.intValue()) {
        Level.SEVERE.intValue() -> "ERROR" to (console.theme["logging.level.error"] ?: Style(color = Color.Red, bold = true))
        Level.WARNING.intValue() -> "WARN" to (console.theme["logging.level.warning"] ?: Style(color = Color.Yellow))
        Level.INFO.intValue() -> "INFO" to (console.theme["logging.level.info"] ?: Style(color = Color.Blue))
        Level.CONFIG.intValue() -> "CONFIG" to Style(color = Color.Cyan)
        Level.FINE.intValue() -> "DEBUG" to (console.theme["logging.level.debug"] ?: Style(color = Color.Green, dim = true))
        Level.FINER.intValue() -> "TRACE" to Style(color = Color.Magenta, dim = true)
        Level.FINEST.intValue() -> "TRACE" to Style(color = Color.Magenta, dim = true)
        else -> level.name.uppercase() to Style.NULL
    }
}

/** Map [LogLevel] back to JUL [Level] for use with rich-friendly logging APIs. */
public fun LogLevel.toJulLevel(): Level = when (this) {
    LogLevel.DEBUG -> Level.FINE
    LogLevel.INFO -> Level.INFO
    LogLevel.WARNING -> Level.WARNING
    LogLevel.ERROR -> Level.SEVERE
    LogLevel.CRITICAL -> Level.SEVERE
}
