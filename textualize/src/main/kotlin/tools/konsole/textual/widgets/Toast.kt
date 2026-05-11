package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.panel.Panel
import tools.konsole.textual.widget.Widget

/** Severity classes for a [Toast]. Mirrors Python textual's `Notification.Severity`. */
public enum class ToastSeverity { Information, Warning, Error }

/**
 * Small notification panel — typically shown floating in the corner of the
 * screen. Mirrors Python textual's `Toast` widget.
 *
 * Phase 9.6 ships the visual; the compositor's overlay placement (so toasts
 * float over other widgets and auto-dismiss after [timeout]) arrives once
 * the compositor's layer/dock system lands. Until then, embed a Toast like
 * any other widget and call [dismiss] manually.
 *
 * @param title bold heading at the top of the toast.
 * @param message body text (supports markup).
 * @param severity colour scheme (information / warning / error).
 * @param timeout auto-dismiss delay in milliseconds. `null` = sticky.
 */
public open class Toast(
    public val title: String = "",
    public val message: String = "",
    public val severity: ToastSeverity = ToastSeverity.Information,
    public val timeout: Long? = 3000L,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var dismissed: Boolean = false
        private set

    /** Mark the toast dismissed. The compositor (Phase 9.7) removes it on the next frame. */
    public fun dismiss() {
        if (dismissed) return
        dismissed = true
        refresh()
    }

    override fun render(): Renderable {
        if (dismissed) return Text("")
        val (border, fg) = colors(severity)
        val body = Text()
        if (title.isNotEmpty()) {
            body.append(title, Style(color = fg, bold = true))
            body.append("\n")
        }
        if (message.isNotEmpty()) {
            for (seg in Markup.parse(message).render(
                tools.konsole.rich.Console.string(width = 60),
                tools.konsole.rich.RenderOptions(maxWidth = 60),
            )) {
                body.append(seg.text, seg.style)
            }
        }
        return Panel(
            renderable = body,
            box = Box.ROUNDED,
            borderStyle = Style(color = border),
        )
    }

    private fun colors(s: ToastSeverity): Pair<Color, Color> = when (s) {
        ToastSeverity.Information -> Color.Cyan to Color.White
        ToastSeverity.Warning -> Color.Yellow to Color.Yellow
        ToastSeverity.Error -> Color.Red to Color.Red
    }
}

/**
 * Hover-triggered text bubble. Mirrors Python textual's `Tooltip`.
 *
 * In textual, a Tooltip is shown next to a widget when its `tooltip`
 * attribute is set and the pointer hovers. Konsole's compositor overlay
 * system (Phase 9.7) will wire that hover binding; for now, attach
 * a Tooltip widget directly and render it when you want the bubble shown.
 */
public open class Tooltip(
    public val text: String,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override fun render(): Renderable {
        val body = Markup.parse(text)
        return Panel(
            renderable = body,
            box = Box.ROUNDED,
            borderStyle = Style(color = Color.Yellow),
        )
    }
}
