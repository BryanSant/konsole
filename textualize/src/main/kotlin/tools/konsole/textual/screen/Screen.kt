package tools.konsole.textual.screen

import tools.konsole.textual.widget.Widget

/**
 * A full-screen modal layer. Mirrors Python textual's `Screen`.
 *
 * Screens stack on the App's screen stack (`push_screen` / `pop_screen` /
 * `switch_screen`). Only the top screen receives input; lower screens are
 * preserved but inactive.
 *
 * A Screen is just a special [Widget] — its [compose] method yields the widgets
 * displayed on this screen.
 */
public abstract class Screen(
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes)
