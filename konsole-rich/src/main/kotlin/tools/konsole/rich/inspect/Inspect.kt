package tools.konsole.rich.inspect

import tools.konsole.rich.Console
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Group
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.panel.Panel
import tools.konsole.rich.table.Table
import tools.konsole.rich.text.Justify
import tools.konsole.core.style.Color
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Reflection-based introspection of an object. Mirrors `rich.inspect`.
 *
 * Renders a panel with the object's class, optional docstring/description, attributes (fields),
 * and methods. Excludes synthetic and bridge members. Includes a specialised path for [Throwable]
 * that shows message + stack trace excerpt.
 */
public class Inspect(
    public val target: Any?,
    public val all: Boolean = false,
    public val help: Boolean = false,
    public val methods: Boolean = false,
    public val docs: Boolean = true,
    public val title: String? = null,
    public val sortFields: Boolean = true,
    public val maxValueLength: Int = 80,
) : Renderable {

    override fun render(console: Console, options: tools.konsole.rich.RenderOptions): Sequence<tools.konsole.rich.Segment> {
        val panel = buildPanel()
        return panel.render(console, options)
    }

    private fun buildPanel(): Panel {
        if (target == null) {
            return Panel(Markup.parse("[dim]null[/]"), title = Text("None"), box = Box.HEAVY)
        }
        val klass = target::class.java
        val titleText = Markup.parse(
            (title ?: "[bold cyan]${escape(klass.simpleName ?: klass.name)}[/]")
        )
        val sub = klass.canonicalName ?: klass.name

        val children = mutableListOf<Renderable>()

        // Header: type-info line.
        children += Markup.parse("[dim]${escape(sub)}[/]")

        // Specialised: Throwable
        if (target is Throwable) {
            children += Markup.parse("\n[bold red]message:[/] ${escape(target.message ?: "<none>")}")
            children += Markup.parse("\n[bold]stack trace[/] [dim](top 5 frames)[/]:")
            for (frame in target.stackTrace.take(5)) {
                children += Markup.parse(
                    "  [magenta]${escape(frame.className)}[/]" +
                        ".[bold]${escape(frame.methodName)}[/]" +
                        " [dim](${escape(frame.fileName ?: "?")}:${frame.lineNumber})[/]"
                )
            }
        }

        // Fields table
        val fieldTable = Table(
            box = null,
            showEdge = false,
            showHeader = false,
            padding = tools.konsole.rich.layout.Padding(0, 1, 0, 0),
        ).apply {
            addColumn(tools.konsole.rich.table.Column(justify = Justify.Right))
            addColumn(tools.konsole.rich.table.Column(justify = Justify.Left))
            addColumn(tools.konsole.rich.table.Column(justify = Justify.Left))
        }

        val fields = collectFields(klass, all)
        if (fields.isNotEmpty()) {
            children += Markup.parse("\n[bold yellow]attributes[/]:")
            for (field in fields) {
                val value = try { field.get(target) } catch (_: Exception) { "<inaccessible>" }
                fieldTable.addRow(
                    Markup.parse("[yellow]${escape(field.name)}[/]"),
                    Markup.parse("[dim]=[/]"),
                    Markup.parse(formatValue(value)),
                )
            }
            children += fieldTable
        }

        // Methods table (optional).
        if (methods) {
            val methodList = collectMethods(klass, all)
            if (methodList.isNotEmpty()) {
                children += Markup.parse("\n[bold yellow]methods[/]:")
                val mTable = Table(
                    box = null,
                    showEdge = false,
                    showHeader = false,
                    padding = tools.konsole.rich.layout.Padding(0, 1, 0, 0),
                ).apply {
                    addColumn(tools.konsole.rich.table.Column(justify = Justify.Left))
                    addColumn(tools.konsole.rich.table.Column(justify = Justify.Left))
                }
                for (m in methodList) {
                    mTable.addRow(
                        Markup.parse("[bold cyan]${escape(m.name)}[/]"),
                        Markup.parse("[dim]${formatSignature(m)}[/]"),
                    )
                }
                children += mTable
            }
        }

        return Panel(
            Group(children),
            title = titleText,
            box = Box.ROUNDED,
            borderStyle = Style(color = Color.Cyan),
        )
    }

    private fun collectFields(klass: Class<*>, includeAll: Boolean): List<Field> {
        val out = mutableListOf<Field>()
        var cur: Class<*>? = klass
        val seen = mutableSetOf<String>()
        while (cur != null && cur != Any::class.java) {
            for (f in cur.declaredFields) {
                if (f.isSynthetic) continue
                if (Modifier.isStatic(f.modifiers)) continue
                if (!includeAll && f.name.startsWith("$")) continue
                if (!includeAll && Modifier.isPrivate(f.modifiers) && !canRead(f)) continue
                if (seen.add(f.name)) {
                    f.trySetAccessible()
                    out += f
                }
            }
            cur = cur.superclass
        }
        return if (sortFields) out.sortedBy { it.name } else out
    }

    private fun collectMethods(klass: Class<*>, includeAll: Boolean): List<Method> {
        return klass.methods
            .filter { !it.isSynthetic && !it.isBridge }
            .filter { includeAll || !it.name.startsWith("$") }
            .filter { includeAll || it.declaringClass != Any::class.java }
            .sortedBy { it.name }
    }

    private fun canRead(f: Field): Boolean = try { f.trySetAccessible(); true } catch (_: Exception) { false }

    private fun formatValue(value: Any?): String {
        val raw = when (value) {
            null -> "[magenta italic]null[/]"
            is String -> "[green]\"${escape(truncate(value))}\"[/]"
            is Char -> "[green]'${escape(value.toString())}'[/]"
            is Number -> "[cyan bold]${value}[/]"
            is Boolean -> if (value) "[green italic]true[/]" else "[red italic]false[/]"
            is Collection<*> -> "[dim]<size=${value.size}>[/] ${escape(truncate(value.toString()))}"
            is Map<*, *> -> "[dim]<size=${value.size}>[/] ${escape(truncate(value.toString()))}"
            is Throwable -> "[red]${escape(value::class.simpleName ?: "Throwable")}: ${escape(value.message ?: "")}[/]"
            else -> "[dim]${escape(truncate(value.toString()))}[/]"
        }
        return raw
    }

    private fun formatSignature(m: Method): String {
        val params = m.parameterTypes.joinToString(", ") { it.simpleName }
        val ret = m.returnType.simpleName
        return "(${params}) -> $ret"
    }

    private fun truncate(s: String): String =
        if (s.length <= maxValueLength) s else s.take(maxValueLength - 1) + "…"

    private fun escape(s: String): String = Markup.escape(s)
}

/** Console.inspect helper. Mirrors rich's `console.inspect(obj)`. */
public fun Console.inspect(
    target: Any?,
    methods: Boolean = false,
    all: Boolean = false,
    title: String? = null,
) {
    print(Inspect(target, methods = methods, all = all, title = title))
}
