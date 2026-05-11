package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/** Visual variant for a [Banner]. */
public enum class BannerSeverity {
    Info,
    Success,
    Warning,
    Error,
}

/**
 * Persistent single-row notification bar. Mirrors the "banner" pattern
 * common in web frameworks — pinned to the top or bottom of a screen
 * with a status message and an optional dismiss `✕` button.
 *
 * Banners are styled by [BannerSeverity] (Info / Success / Warning / Error).
 * Click the `✕` (or call [dismiss]) to remove. A [Dismissed] message is
 * posted; the App owner is expected to detach the banner from the DOM.
 *
 * Banners are not Toasts: Toasts are time-limited overlay notifications;
 * Banners are persistent inline rows. Use [Banner] for "you have unsaved
 * changes" or "build failed" style messages that should stay until the
 * user acts.
 */
public open class Banner(
    initialMessage: String,
    public val severity: BannerSeverity = BannerSeverity.Info,
    public val dismissible: Boolean = true,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var message: String = initialMessage
        private set

    public var dismissed: Boolean = false
        private set

    /** Replace the message text. Re-renders on next frame. */
    public fun update(newMessage: String) {
        message = newMessage
        refresh()
    }

    /** Dismiss the banner. Posts [Dismissed]; the App is expected to detach it. */
    public fun dismiss() {
        if (dismissed) return
        dismissed = true
        post(Dismissed(this))
    }

    override suspend fun onEvent(event: tools.konsole.textual.events.Event) {
        if (!dismissible) return
        if (event is tools.konsole.textual.events.Click) {
            // Click the trailing close button area (last 3 cells of the row).
            val region = lastRegion ?: return
            if (event.x >= region.right - 3 && event.y == region.y) {
                dismiss()
                event.stop()
            }
        }
    }

    override fun render(): Renderable {
        val (bg, fg, icon) = when (severity) {
            BannerSeverity.Info -> Triple(Color.Blue, Color.White, "ℹ")
            BannerSeverity.Success -> Triple(Color.Green, Color.Black, "✓")
            BannerSeverity.Warning -> Triple(Color.Yellow, Color.Black, "⚠")
            BannerSeverity.Error -> Triple(Color.Red, Color.White, "✗")
        }
        val body = Text()
        val baseStyle = Style(bgcolor = bg, color = fg)
        body.append(" $icon  ", baseStyle.copy(bold = true))
        // Allow simple markup in the message.
        val parsed = Markup.parse(message)
        body.append(parsed)
        if (dismissible) {
            body.append("  ", baseStyle)
            body.append(" ✕ ", baseStyle.copy(bold = true))
        }
        return body
    }

    /** Posted when the banner is dismissed (by click or [dismiss]). */
    public data class Dismissed(val banner: Banner) : Message()
}
