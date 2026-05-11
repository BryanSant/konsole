package tools.konsole.rich

import tools.konsole.core.ColorSystem

import tools.konsole.core.Command
import tools.konsole.core.EndLink
import tools.konsole.core.StartLink
import tools.konsole.core.execute
import tools.konsole.rich.text.Justify
import tools.konsole.rich.text.Overflow
import tools.konsole.core.style.Print
import tools.konsole.core.style.ResetColor
import tools.konsole.core.style.SetStyle

/**
 * The end-to-end render pipeline:
 *   1. resolve — fill null Style fields by inheriting from the base style + record buffer if [Console.record].
 *   2. wrap    — break each line at [maxWidth]; apply justify/overflow.
 *   3. crop    — drop excess rows if requested.
 *   4. emit    — coalesce adjacent same-style segments; produce SetStyle/Print/ResetColor commands.
 *   5. write   — wrap the whole batch in synchronizedUpdate for atomic display.
 */
internal object Pipeline {

    fun run(
        console: Console,
        renderable: Renderable,
        options: RenderOptions,
        terminalEnd: String,
        baseStyle: Style?,
        crop: Boolean,
    ) {
        val width = options.maxWidth.coerceAtLeast(1)
        val raw: Sequence<Segment> = renderable.render(console, options)
        val resolved: Sequence<Segment> = SegmentResolver.resolve(raw, baseStyle ?: Style.NULL)
        // Buffer wrap+crop into a list so we know the total before emit (and so split-and-justify can
        // make decisions per-line without infinite lookahead).
        val wrapped: List<Segment> = SegmentSplitter.splitAndJustify(
            segments = resolved,
            maxWidth = width,
            justify = options.justify,
            overflow = options.overflow,
            noWrap = options.noWrap,
        )
        val cropped: List<Segment> = if (crop && options.height != null) {
            limitLines(wrapped, options.height)
        } else {
            wrapped
        }
        if (console.record) {
            console.recordBuffer.addAll(cropped)
            // Pipeline writes terminalEnd ("\n" by default) directly to the writer without
            // running through the segment buffer. Mirror it into the record buffer so exports
            // preserve line breaks between successive print() calls.
            for (ch in terminalEnd) if (ch == '\n') console.recordBuffer.add(Segment.LINE)
        }

        val commands: List<Command> = SegmentEmitter.emit(cropped, console.colorSystem)
        if (commands.isEmpty() && terminalEnd.isEmpty()) return

        val terminal = console.terminal
        val writer = console.writer

        val emitBlock: () -> Unit = {
            if (commands.isNotEmpty()) writer.execute(*commands.toTypedArray())
            if (terminalEnd.isNotEmpty()) {
                writer.write(terminalEnd)
                writer.flush()
            }
        }

        if (terminal != null) {
            terminal.synchronizedUpdate(emitBlock)
        } else {
            emitBlock()
        }
    }

    private fun limitLines(segments: List<Segment>, max: Int): List<Segment> {
        if (max <= 0) return emptyList()
        var lines = 0
        val out = mutableListOf<Segment>()
        for (seg in segments) {
            out += seg
            if (seg.control is Control.NewLine) {
                lines += 1
                if (lines >= max) break
            }
        }
        return out
    }
}

/** Fills null Style fields from a base, threading inherited style through nested renders. */
internal object SegmentResolver {
    fun resolve(input: Sequence<Segment>, base: Style): Sequence<Segment> = sequence {
        for (seg in input) {
            if (seg.control is Control.NewLine) {
                yield(seg)
                continue
            }
            if (seg.text.isEmpty() && seg.control == null) continue
            val merged: Style? = when {
                seg.style == null -> if (base.isNull) null else base
                else -> if (base.isNull) seg.style else base + seg.style
            }
            yield(Segment(seg.text, merged, seg.control))
        }
    }
}

/**
 * Wrap each line at [maxWidth], applying justify/overflow. Materializes into a List so callers
 * can inspect total height for cropping.
 *
 * Convention: a LINE marker in the OUTPUT means "line break here" (not "end of stream"). The
 * splitter never appends a trailing LINE — terminating the final line is the caller's job
 * (typically via `terminalEnd` on `print(...)`).
 */
internal object SegmentSplitter {
    fun splitAndJustify(
        segments: Sequence<Segment>,
        maxWidth: Int,
        justify: Justify,
        overflow: Overflow,
        noWrap: Boolean,
    ): List<Segment> {
        val out = mutableListOf<Segment>()
        var line = mutableListOf<Segment>()
        var lineCells = 0
        var firstLine = true

        fun emitBufferedLine() {
            if (!firstLine) out += Segment.LINE
            firstLine = false
            val wrapped: List<Segment> = if (noWrap || line.isEmpty()) {
                line
            } else {
                wrapLineSegments(line, maxWidth, overflow)
            }
            // wrapLineSegments may have inserted internal LINE markers at wrap points.
            // Apply justify per sub-line; emit LINE between, but not after, sub-lines.
            var subline = mutableListOf<Segment>()
            var sublineCells = 0
            var firstSub = true
            for (s in wrapped) {
                if (s.control is Control.NewLine) {
                    if (!firstSub) out += Segment.LINE
                    firstSub = false
                    applyJustifyOnce(subline, sublineCells, maxWidth, justify, out)
                    subline = mutableListOf()
                    sublineCells = 0
                } else {
                    subline += s
                    sublineCells += s.cellLength
                }
            }
            if (subline.isNotEmpty() || firstSub) {
                if (!firstSub) out += Segment.LINE
                applyJustifyOnce(subline, sublineCells, maxWidth, justify, out)
            }
            line = mutableListOf()
            lineCells = 0
        }

        for (seg in segments) {
            if (seg.control is Control.NewLine) {
                emitBufferedLine()
            } else {
                line += seg
                lineCells += seg.cellLength
            }
        }
        if (line.isNotEmpty() || !firstLine) emitBufferedLine()
        return out
    }

    private fun applyJustifyOnce(
        line: List<Segment>,
        cells: Int,
        maxWidth: Int,
        justify: Justify,
        out: MutableList<Segment>,
    ) {
        when (justify) {
            Justify.Default, Justify.Left -> {
                out.addAll(line)
            }
            Justify.Right -> {
                val pad = (maxWidth - cells).coerceAtLeast(0)
                if (pad > 0) out += Segment(" ".repeat(pad))
                out.addAll(line)
            }
            Justify.Center -> {
                val pad = (maxWidth - cells).coerceAtLeast(0)
                val left = pad / 2
                val right = pad - left
                if (left > 0) out += Segment(" ".repeat(left))
                out.addAll(line)
                if (right > 0) out += Segment(" ".repeat(right))
            }
            Justify.Full -> {
                // Full justify is non-trivial; for v1 fall back to left.
                out.addAll(line)
            }
        }
    }

    private fun wrapLineSegments(
        segments: List<Segment>,
        maxWidth: Int,
        overflow: Overflow,
    ): List<Segment> {
        // Build a single string with style indices, then break on whitespace/word boundaries.
        // Lightweight algorithm: greedy word wrap with overflow handling.
        val out = mutableListOf<Segment>()
        var col = 0
        for (seg in segments) {
            val text = seg.text
            if (text.isEmpty()) continue
            var i = 0
            while (i < text.length) {
                val remaining = maxWidth - col
                if (remaining <= 0) {
                    if (overflow == Overflow.Crop || overflow == Overflow.Ellipsis) {
                        // Drop the rest of this line.
                        return out
                    }
                    out += Segment.LINE
                    col = 0
                    continue
                }
                if (text.length - i <= remaining) {
                    out += Segment(text.substring(i), seg.style, seg.control)
                    col += text.length - i
                    i = text.length
                } else {
                    // Find a wrap point (last whitespace within window) to avoid mid-word breaks.
                    val window = text.substring(i, i + remaining)
                    val lastSpace = window.lastIndexOf(' ')
                    val cut = if (lastSpace > 0) lastSpace else window.length
                    out += Segment(text.substring(i, i + cut), seg.style, seg.control)
                    col += cut
                    i += cut
                    // Skip the space we wrapped on
                    if (i < text.length && text[i] == ' ') i += 1
                    if (overflow == Overflow.Ellipsis && col >= maxWidth - 1) {
                        // Replace the trailing chars with an ellipsis on this line, then crop.
                        if (out.isNotEmpty()) {
                            val last = out.last()
                            val trimmed = if (last.text.length >= 1) last.text.dropLast(1) + "…" else "…"
                            out[out.lastIndex] = last.copy(text = trimmed)
                        }
                        return out
                    }
                    out += Segment.LINE
                    col = 0
                }
            }
        }
        return out
    }
}

/**
 * Coalesces adjacent same-style segments into a single SetStyle command, emits per-line ResetColor,
 * and translates [Control.Link]/[Control.LinkEnd] to OSC 8 commands.
 */
internal object SegmentEmitter {

    /** Strip styles for NONE color system — emit raw text + newlines only. */
    private fun emitPlain(segments: List<Segment>): List<Command> {
        val out = mutableListOf<Command>()
        val pending = StringBuilder()
        for (seg in segments) {
            when {
                seg.control is Control.NewLine -> {
                    if (pending.isNotEmpty()) { out += Print(pending.toString()); pending.clear() }
                    out += Print("\n")
                }
                seg.control != null -> { /* ignore link/meta in NONE mode */ }
                seg.text.isEmpty() -> { /* skip */ }
                else -> pending.append(seg.text)
            }
        }
        if (pending.isNotEmpty()) out += Print(pending.toString())
        return out
    }


    fun emit(segments: List<Segment>, colorSystem: ColorSystem): List<Command> {
        if (segments.isEmpty()) return emptyList()
        if (colorSystem == ColorSystem.None) return emitPlain(segments)
        val out = mutableListOf<Command>()
        var currentStyle: Style? = null
        var currentLink: String? = null
        val pendingText = StringBuilder()

        fun flushText() {
            if (pendingText.isEmpty()) return
            out += Print(pendingText.toString())
            pendingText.clear()
        }

        for (seg in segments) {
            when {
                seg.control is Control.NewLine -> {
                    flushText()
                    if (currentLink != null) {
                        out += EndLink
                        currentLink = null
                    }
                    if (currentStyle != null && !currentStyle.isNull) {
                        out += ResetColor
                        currentStyle = null
                    }
                    out += Print("\n")
                }
                seg.control is Control.Link -> {
                    flushText()
                    if (currentLink != null) out += EndLink
                    out += StartLink(seg.control.uri)
                    currentLink = seg.control.uri
                }
                seg.control is Control.LinkEnd -> {
                    flushText()
                    if (currentLink != null) {
                        out += EndLink
                        currentLink = null
                    }
                }
                seg.text.isEmpty() -> {
                    // skip
                }
                else -> {
                    val style = seg.style
                    val styleLink = style?.link
                    if (style != currentStyle) {
                        flushText()
                        if (style == null || style.isNull) {
                            if (currentStyle != null) {
                                out += ResetColor
                            }
                        } else {
                            out += SetStyle(style.toContentStyle(colorSystem))
                        }
                        currentStyle = style
                    }
                    if (styleLink != currentLink) {
                        flushText()
                        if (currentLink != null) out += EndLink
                        if (styleLink != null) out += StartLink(styleLink)
                        currentLink = styleLink
                    }
                    pendingText.append(seg.text)
                }
            }
        }
        flushText()
        if (currentLink != null) out += EndLink
        if (currentStyle != null && !currentStyle!!.isNull) out += ResetColor
        return out
    }
}
