package tools.konsole.rich.table

import tools.konsole.rich.Style

/**
 * A row in a [Table]. End-section rows can carry a divider via [endSection].
 */
public data class Row(
    public val style: Style? = null,
    public val endSection: Boolean = false,
)
