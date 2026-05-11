package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.spinner.Spinner
import tools.konsole.textual.widget.Widget

/**
 * An animated spinner — used to indicate work in progress. Mirrors Python
 * textual's `LoadingIndicator`. Defaults to the `dots` spinner from rich's
 * full 73-spinner catalogue.
 *
 * @param spinnerName any of [tools.konsole.rich.spinner.SPINNERS] keys.
 * @param style optional style applied to the spinner glyph.
 */
public open class LoadingIndicator(
    public val spinnerName: String = "dots",
    public val style: Style? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    override fun render(): Renderable = Spinner(spinnerName, text = null, style = style)
}
