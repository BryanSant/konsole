package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.syntax.SyntaxTheme
import tools.konsole.rich.syntax.Token
import tools.konsole.rich.syntax.TreeSitterLanguages
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Scrollable
import tools.konsole.textual.widget.Widget

/** A `(row, column)` location in a [Document]. Both are zero-based. */
public data class Location(public val row: Int, public val column: Int) : Comparable<Location> {
    override fun compareTo(other: Location): Int =
        if (row != other.row) row.compareTo(other.row) else column.compareTo(other.column)

    public companion object { public val ZERO: Location = Location(0, 0) }
}

/**
 * An ordered range `[start, end]` inside a [Document]. When `start == end`,
 * the selection is collapsed to a single cursor position with no highlighted
 * text. Mirrors Python textual's `Selection`.
 */
public data class Selection(public val start: Location, public val end: Location) {
    public val isCollapsed: Boolean get() = start == end
    /** Min-then-max ordering: returns `(low, high)` regardless of how the user dragged. */
    public fun ordered(): Pair<Location, Location> =
        if (start <= end) start to end else end to start

    public companion object { public val ZERO: Selection = Selection(Location.ZERO, Location.ZERO) }
}

/**
 * Text buffer for [TextArea]. Line-addressable; mutations expressed as
 * `(coordinate, edit)` pairs. Mirrors textual's `Document` ABC.
 *
 * No syntax-tree integration in Phase 9.7 — tree-sitter is a v2 scope-cut
 * per the design plan. A future `SyntaxDocument` subclass can layer on
 * incremental parsing without changing this base API.
 */
public class Document(initial: String = "") {

    private val _lines: MutableList<StringBuilder> = mutableListOf<StringBuilder>().apply {
        if (initial.isEmpty()) add(StringBuilder())
        else initial.split("\n").forEach { line -> add(StringBuilder(line)) }
    }

    /** The text content, joined with `\n`. */
    public val text: String get() = _lines.joinToString("\n")

    /** Number of lines (always ≥ 1). */
    public val lineCount: Int get() = _lines.size

    public fun line(index: Int): String = _lines.getOrNull(index)?.toString() ?: ""

    public fun lines(): List<String> = _lines.map { it.toString() }

    /**
     * Replace the substring `[start, end)` with [replacement]. Returns the new
     * cursor location (end of the inserted text).
     */
    public fun replace(start: Location, end: Location, replacement: String): Location {
        val (a, b) = if (start <= end) start to end else end to start
        // Capture suffix of the last affected line.
        val tailRow = b.row.coerceAtMost(_lines.lastIndex)
        val tailCol = b.column.coerceAtMost(_lines[tailRow].length)
        val suffix = _lines[tailRow].substring(tailCol)
        // Trim prefix of the first affected line.
        val headRow = a.row.coerceAtMost(_lines.lastIndex)
        val headCol = a.column.coerceAtMost(_lines[headRow].length)
        val prefix = _lines[headRow].substring(0, headCol)
        // Remove the range of fully-or-partially affected lines.
        for (i in tailRow downTo headRow) _lines.removeAt(i)
        // Insert the new content.
        val replaced = (prefix + replacement + suffix).split("\n")
        for ((offset, line) in replaced.withIndex()) {
            _lines.add(headRow + offset, StringBuilder(line))
        }
        // Compute new cursor location (one past the inserted text).
        val insertedLines = replacement.split("\n")
        val newRow = headRow + insertedLines.lastIndex
        val newCol = if (insertedLines.size == 1) headCol + replacement.length
                     else insertedLines.last().length
        return Location(newRow, newCol)
    }

    /** Insert [text] at [coord]. Returns the new cursor location. */
    public fun insert(coord: Location, text: String): Location = replace(coord, coord, text)

    /** Delete the range `[start, end)`. */
    public fun delete(start: Location, end: Location): Location = replace(start, end, "")

    /** Slice the document between [start] and [end] (inclusive of start, exclusive of end). */
    public fun substring(start: Location, end: Location): String {
        val (a, b) = if (start <= end) start to end else end to start
        if (a == b) return ""
        if (a.row == b.row) return _lines[a.row].substring(a.column, b.column.coerceAtMost(_lines[a.row].length))
        val sb = StringBuilder()
        sb.append(_lines[a.row].substring(a.column))
        for (r in a.row + 1 until b.row) { sb.append('\n'); sb.append(_lines[r]) }
        sb.append('\n').append(_lines[b.row].substring(0, b.column.coerceAtMost(_lines[b.row].length)))
        return sb.toString()
    }

    public fun lineLength(row: Int): Int = _lines.getOrNull(row)?.length ?: 0

    public fun endLocation(): Location = Location(_lines.lastIndex, _lines.last().length)
}

/**
 * Multi-line text editor widget. Mirrors Python textual's `TextArea`.
 *
 * Phase 9.7 ships the editing core: insert / delete / cursor navigation /
 * selection / clear. Per the design plan, **tree-sitter integration is
 * deferred to v2**; this widget renders without syntax highlighting.
 *
 * Bindings (textual-compatible):
 *   - arrow keys           — move cursor (with shift to extend selection — Phase 9.8)
 *   - home / end           — start / end of line
 *   - ctrl+home / ctrl+end — start / end of document
 *   - pageup / pagedown    — viewport jump (Phase 9.7 just moves cursor)
 *   - backspace            — delete char before cursor (or selection)
 *   - delete               — delete char after cursor (or selection)
 *   - enter                — insert newline
 *   - any printable char   — insert at cursor
 *
 * @param initial text to populate the document with.
 * @param readOnly when true, edits are ignored (cursor still moves).
 * @param showLineNumbers prefix each line with its 1-based row number.
 */
public open class TextArea(
    initial: String = "",
    public val readOnly: Boolean = false,
    public val showLineNumbers: Boolean = false,
    /**
     * Optional tree-sitter language for syntax highlighting. Accepted names
     * are the same aliases [TreeSitterLanguages.byName] understands —
     * `"kotlin"`, `"kt"`, `"java"`, `"python"`, `"py"`, `"json"`, `"bash"`,
     * `"sh"`, etc. Pass `null` (the default) for a plain text editor.
     */
    public val language: String? = null,
    /**
     * Color scheme for syntax tokens. Defaults to the dark ANSI theme;
     * pass a custom theme to override colours or backgrounds.
     */
    public val syntaxTheme: SyntaxTheme = SyntaxTheme.ANSI_DARK,
    /**
     * Soft-wrap long lines to the widget's assigned width instead of
     * scrolling horizontally. Wrapped continuations don't update the
     * document — only the rendering. Cursor navigation stays
     * line-and-column based; users can still arrow off the visible end
     * of a wrapped row to land on the next document line.
     */
    public val wordWrap: Boolean = false,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes), Scrollable {

    public val document: Document = Document(initial)

    /** Tree-sitter lexer for [language], or null if no highlighting requested. */
    private val lexer = language?.let { TreeSitterLanguages.byName(it) }

    /** Cache: per-character style indexed by document-wide char offset. */
    private var tokenStyleCache: Array<Style?>? = null
    private var tokenStyleCacheText: String? = null

    private fun ensureTokenStyles(text: String): Array<Style?>? {
        val lex = lexer ?: return null
        if (tokenStyleCacheText === text && tokenStyleCache != null) return tokenStyleCache
        val styles = arrayOfNulls<Style>(text.length)
        try {
            for (tok in lex.tokenize(text)) {
                val s = syntaxTheme[tok.type]
                if (s === Style.NULL) continue
                val end = tok.end.coerceAtMost(text.length)
                for (i in tok.start until end) styles[i] = s
            }
        } catch (_: Throwable) {
            return null  // lexer failed (e.g. native lib missing) — fall back to plain
        }
        tokenStyleCache = styles
        tokenStyleCacheText = text
        return styles
    }

    public var cursor: Location = Location.ZERO
        private set

    public var selection: Selection = Selection(cursor, cursor)
        private set

    /** Current incremental-search query, or null when search is inactive. */
    public var searchQuery: String? = null
        private set

    /** All matches of the active [searchQuery] in document order. Empty when search is off. */
    public var searchMatches: List<Selection> = emptyList()
        private set

    /** Index of the currently-focused match within [searchMatches], or -1 if none. */
    public var activeMatchIndex: Int = -1
        private set

    override val canFocus: Boolean get() = true

    override var scrollX: Int = 0
    override var scrollY: Int = 0
    override val contentWidth: Int get() = (0 until document.lineCount).maxOf { document.lineLength(it) }
    override val contentHeight: Int get() = document.lineCount

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("up", "cursor_up", show = false),
            Binding("down", "cursor_down", show = false),
            Binding("left", "cursor_left", show = false),
            Binding("right", "cursor_right", show = false),
            Binding("home", "cursor_line_start", show = false),
            Binding("end", "cursor_line_end", show = false),
            Binding("ctrl+home", "cursor_document_start", show = false),
            Binding("ctrl+end", "cursor_document_end", show = false),
            Binding("backspace", "delete_left", show = false),
            Binding("delete", "delete_right", show = false),
            Binding("enter", "newline", show = false),
        )
    )

    // Binding-dispatched actions
    @Suppress("unused") public fun action_cursor_up() { moveCursorUp() }
    @Suppress("unused") public fun action_cursor_down() { moveCursorDown() }
    @Suppress("unused") public fun action_cursor_left() { moveCursorLeft() }
    @Suppress("unused") public fun action_cursor_right() { moveCursorRight() }
    @Suppress("unused") public fun action_cursor_line_start() { moveCursorLineStart() }
    @Suppress("unused") public fun action_cursor_line_end() { moveCursorLineEnd() }
    @Suppress("unused") public fun action_cursor_document_start() { moveCursorDocumentStart() }
    @Suppress("unused") public fun action_cursor_document_end() { moveCursorDocumentEnd() }
    @Suppress("unused") public fun action_delete_left() { deleteLeft() }
    @Suppress("unused") public fun action_delete_right() { deleteRight() }
    @Suppress("unused") public fun action_newline() { insert("\n") }

    /**
     * Intercept printable Key events as character insertion. Navigation and
     * editing keys flow through bindings → actions instead.
     */
    override suspend fun onEvent(event: tools.konsole.textual.events.Event) {
        if (readOnly) return
        if (event !is tools.konsole.textual.events.Key) return
        if (event.modifiers.bits != 0) return
        val code = event.code
        if (code is tools.konsole.core.event.KeyCode.Char) {
            val ch = code.c
            if (ch.code >= 0x20 && ch.code != 0x7F) {
                insert(ch.toString())
                event.stop()
            }
        }
    }

    /** Text content of the document. */
    public val text: String get() = document.text

    /** Replace the entire document with [newText]; cursor jumps to the end. */
    public open fun load(newText: String) {
        document.replace(Location.ZERO, document.endLocation(), newText)
        cursor = document.endLocation()
        selection = Selection(cursor, cursor)
        post(Changed(this, text))
        refresh()
    }

    /** Insert [text] at the current cursor (replacing any selection). */
    public open fun insert(text: String) {
        if (readOnly) return
        val (a, b) = selection.ordered()
        cursor = document.replace(a, b, text)
        selection = Selection(cursor, cursor)
        post(Changed(this, this.text))
        refresh()
    }

    /** Delete one character to the left of the cursor (or the selection). */
    public open fun deleteLeft() {
        if (readOnly) return
        if (!selection.isCollapsed) { deleteSelection(); return }
        if (cursor == Location.ZERO) return
        val newStart = if (cursor.column > 0) Location(cursor.row, cursor.column - 1)
                       else Location(cursor.row - 1, document.lineLength(cursor.row - 1))
        cursor = document.delete(newStart, cursor)
        selection = Selection(cursor, cursor)
        post(Changed(this, text))
        refresh()
    }

    /** Delete one character to the right of the cursor (or the selection). */
    public open fun deleteRight() {
        if (readOnly) return
        if (!selection.isCollapsed) { deleteSelection(); return }
        val endLoc = document.endLocation()
        if (cursor == endLoc) return
        val newEnd = if (cursor.column < document.lineLength(cursor.row)) Location(cursor.row, cursor.column + 1)
                     else Location(cursor.row + 1, 0)
        cursor = document.delete(cursor, newEnd)
        selection = Selection(cursor, cursor)
        post(Changed(this, text))
        refresh()
    }

    private fun deleteSelection() {
        val (a, b) = selection.ordered()
        cursor = document.delete(a, b)
        selection = Selection(cursor, cursor)
        post(Changed(this, text))
        refresh()
    }

    public fun moveCursorLeft() { setCursor(stepLeft(cursor)) }
    public fun moveCursorRight() { setCursor(stepRight(cursor)) }
    public fun moveCursorUp() {
        if (cursor.row == 0) return
        setCursor(Location(cursor.row - 1, cursor.column.coerceAtMost(document.lineLength(cursor.row - 1))))
    }
    public fun moveCursorDown() {
        if (cursor.row >= document.lineCount - 1) return
        setCursor(Location(cursor.row + 1, cursor.column.coerceAtMost(document.lineLength(cursor.row + 1))))
    }
    public fun moveCursorLineStart() { setCursor(Location(cursor.row, 0)) }
    public fun moveCursorLineEnd() { setCursor(Location(cursor.row, document.lineLength(cursor.row))) }
    public fun moveCursorDocumentStart() { setCursor(Location.ZERO) }
    public fun moveCursorDocumentEnd() { setCursor(document.endLocation()) }

    private fun stepLeft(from: Location): Location = when {
        from.column > 0 -> Location(from.row, from.column - 1)
        from.row > 0 -> Location(from.row - 1, document.lineLength(from.row - 1))
        else -> Location.ZERO
    }
    private fun stepRight(from: Location): Location {
        val maxCol = document.lineLength(from.row)
        return when {
            from.column < maxCol -> Location(from.row, from.column + 1)
            from.row < document.lineCount - 1 -> Location(from.row + 1, 0)
            else -> from
        }
    }

    public fun setCursor(loc: Location) {
        if (loc == cursor) return
        cursor = loc
        selection = Selection(loc, loc)
        post(SelectionChanged(this, selection))
        refresh()
    }

    /** Set [selection] to an explicit range. Cursor lands on [Selection.end]. */
    public fun select(selection: Selection) {
        this.selection = selection
        this.cursor = selection.end
        post(SelectionChanged(this, selection))
        refresh()
    }

    /** Select everything. */
    public fun selectAll() {
        select(Selection(Location.ZERO, document.endLocation()))
    }

    /**
     * Start (or update) an incremental search. All occurrences of [query]
     * (case-sensitive by default) are highlighted in the editor; the cursor
     * jumps to the first match. Pass an empty string or `null` to clear.
     */
    public fun search(query: String?, ignoreCase: Boolean = false) {
        if (query.isNullOrEmpty()) { clearSearch(); return }
        searchQuery = query
        searchMatches = findAll(query, ignoreCase)
        activeMatchIndex = if (searchMatches.isEmpty()) -1 else 0
        searchMatches.firstOrNull()?.let { setCursor(it.start) }
        refresh()
    }

    /** Clear the active search highlight. */
    public fun clearSearch() {
        searchQuery = null
        searchMatches = emptyList()
        activeMatchIndex = -1
        refresh()
    }

    /** Move the cursor to the next match, wrapping around. No-op if no matches. */
    public fun nextMatch() {
        if (searchMatches.isEmpty()) return
        activeMatchIndex = (activeMatchIndex + 1) % searchMatches.size
        setCursor(searchMatches[activeMatchIndex].start)
    }

    /** Move the cursor to the previous match, wrapping around. */
    public fun previousMatch() {
        if (searchMatches.isEmpty()) return
        activeMatchIndex = if (activeMatchIndex <= 0) searchMatches.lastIndex else activeMatchIndex - 1
        setCursor(searchMatches[activeMatchIndex].start)
    }

    private fun findAll(query: String, ignoreCase: Boolean): List<Selection> {
        if (query.isEmpty()) return emptyList()
        val results = mutableListOf<Selection>()
        for (rowIdx in 0 until document.lineCount) {
            val line = document.line(rowIdx)
            var fromCol = 0
            while (fromCol <= line.length) {
                val idx = line.indexOf(query, fromCol, ignoreCase)
                if (idx < 0) break
                results += Selection(Location(rowIdx, idx), Location(rowIdx, idx + query.length))
                fromCol = idx + query.length.coerceAtLeast(1)
            }
        }
        return results
    }

    override fun render(): Renderable {
        val text = Text()
        val cursorStyle = Style(color = Color.Black, bgcolor = Color.White)
        val (selStart, selEnd) = selection.ordered()
        val gutterStyle = Style(color = Color.DarkGrey, dim = true)
        val gutterWidth = if (showLineNumbers) document.lineCount.toString().length + 1 else 0
        val matchStyle = Style(bgcolor = Color.Rgb(0x80, 0x60, 0x00))   // muted amber
        val activeMatchStyle = Style(bgcolor = Color.Yellow, color = Color.Black)

        // Compute per-char syntax-token styles for the whole document up front.
        val fullText = document.text
        val syntaxStyles = ensureTokenStyles(fullText)
        var globalOffset = 0

        // Word-wrap: assigned region width minus the gutter is the wrap column.
        val wrapWidth = if (wordWrap) {
            val region = lastRegion
            if (region != null) (region.width - gutterWidth).coerceAtLeast(1) else 0
        } else 0

        for (rowIdx in 0 until document.lineCount) {
            if (showLineNumbers) {
                text.append("${(rowIdx + 1).toString().padStart(gutterWidth - 1)} ", gutterStyle)
            }
            val line = document.line(rowIdx)
            var visualCol = 0
            for (colIdx in 0..line.length) {
                val here = Location(rowIdx, colIdx)
                val isCursor = hasFocus && here == cursor && selection.isCollapsed
                val isSelected = here in selStart..selEnd && !selection.isCollapsed
                val matchStyleAt = matchStyleAt(here)
                if (colIdx < line.length) {
                    val ch = line[colIdx].toString()
                    val syntaxStyle = syntaxStyles?.getOrNull(globalOffset)
                    val style = when {
                        isCursor -> cursorStyle
                        isSelected -> Style(bgcolor = Color.Blue)
                        matchStyleAt === activeMatchStyle -> activeMatchStyle
                        matchStyleAt === matchStyle -> matchStyle
                        syntaxStyle != null -> syntaxStyle
                        else -> Style.NULL
                    }
                    text.append(ch, style)
                    globalOffset += 1
                    visualCol += 1
                    // Soft-wrap: insert a newline at the wrap column. The next
                    // visible row continues this logical line; document cursor
                    // navigation is unaffected.
                    if (wordWrap && wrapWidth > 0 && visualCol == wrapWidth && colIdx < line.length - 1) {
                        text.append("\n")
                        if (showLineNumbers) {
                            text.append(" ".repeat(gutterWidth), gutterStyle)
                        }
                        visualCol = 0
                    }
                } else if (isCursor) {
                    // Cursor at end-of-line: render a blank cursor block.
                    text.append(" ", cursorStyle)
                }
            }
            if (rowIdx < document.lineCount - 1) {
                text.append("\n")
                globalOffset += 1  // newline byte in the joined document text
            }
        }
        return text
    }

    private fun matchStyleAt(loc: Location): Style? {
        if (searchMatches.isEmpty()) return null
        for ((i, m) in searchMatches.withIndex()) {
            if (m.start.row == loc.row && loc.column >= m.start.column && loc.column < m.end.column) {
                return if (i == activeMatchIndex)
                    Style(bgcolor = Color.Yellow, color = Color.Black)
                else
                    Style(bgcolor = Color.Rgb(0x80, 0x60, 0x00))
            }
        }
        return null
    }

    private operator fun ClosedRange<Location>.contains(loc: Location): Boolean =
        loc >= start && loc <= endInclusive

    /** Emitted whenever the document text changes. */
    public data class Changed(val textArea: TextArea, val text: String) : Message()

    /** Emitted whenever the selection (cursor) moves. */
    public data class SelectionChanged(val textArea: TextArea, val selection: Selection) : Message()
}
