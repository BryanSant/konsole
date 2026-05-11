package tools.konsole.textual.widgets

/**
 * A simple single-line label. Mirrors Python textual's `Label` — convenience
 * subclass of [Static] for the common case of displaying text.
 *
 * ```
 * yield Label("Hello, world!")
 * yield Label("[bold red]Important![/]")  // markup
 * ```
 */
public open class Label(
    text: String = "",
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Static(text, id, classes)
