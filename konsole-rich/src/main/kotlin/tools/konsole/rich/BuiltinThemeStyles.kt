package tools.konsole.rich

import tools.konsole.core.style.Color

/**
 * The built-in style names rich provides out of the box.
 * Subset chosen to match Python rich's `default_styles` for source-compatible markup.
 */
internal val BuiltinThemeStyles: Map<String, Style> = mapOf(
    "none" to Style.NULL,
    "reset" to Style.NULL,

    "dim" to Style(dim = true),
    "bright" to Style(dim = false),
    "bold" to Style(bold = true),
    "italic" to Style(italic = true),
    "underline" to Style(underline = true),
    "strike" to Style(strike = true),
    "blink" to Style(blink = true),
    "reverse" to Style(reverse = true),

    // Semantic
    "ok" to Style(color = Color.Green),
    "info" to Style(color = Color.Cyan),
    "warning" to Style(color = Color.Yellow),
    "warn" to Style(color = Color.Yellow),
    "error" to Style(color = Color.Red, bold = true),
    "danger" to Style(color = Color.Red, bold = true),
    "critical" to Style(color = Color.White, bgcolor = Color.Red, bold = true),
    "success" to Style(color = Color.Green, bold = true),

    // Logging
    "log.level" to Style(bold = true),
    "log.time" to Style(color = Color.Cyan, dim = true),
    "log.message" to Style.NULL,
    "log.path" to Style(dim = true),
    "logging.level.notset" to Style(dim = true),
    "logging.level.debug" to Style(color = Color.Green, dim = true),
    "logging.level.info" to Style(color = Color.Blue),
    "logging.level.warning" to Style(color = Color.Red),
    "logging.level.error" to Style(color = Color.Red, bold = true),
    "logging.level.critical" to Style(color = Color.Red, bold = true, reverse = true),
    "logging.keyword" to Style(bold = true, color = Color.Yellow),

    // Repr / pretty
    "repr.tag_name" to Style(color = Color.Magenta, bold = true),
    "repr.attrib_name" to Style(color = Color.Yellow, italic = false),
    "repr.attrib_equal" to Style(bold = true),
    "repr.attrib_value" to Style(color = Color.Magenta, italic = false),
    "repr.number" to Style(color = Color.Cyan, bold = true, italic = false),
    "repr.bool_true" to Style(color = Color.Green, italic = true),
    "repr.bool_false" to Style(color = Color.Red, italic = true),
    "repr.none" to Style(color = Color.Magenta, italic = true),
    "repr.url" to Style(color = Color.Blue, italic = false, bold = false, underline = true),
    "repr.uuid" to Style(color = Color.Yellow, bold = false),
    "repr.call" to Style(color = Color.Magenta, bold = true),
    "repr.path" to Style(color = Color.Magenta),
    "repr.filename" to Style(color = Color.Magenta),
    "repr.str" to Style(color = Color.Green, italic = false, bold = false),
    "repr.brace" to Style(bold = true),
    "repr.comma" to Style(bold = true),
    "repr.ipv4" to Style(bold = true, color = Color.Cyan),
    "repr.ipv6" to Style(bold = true, color = Color.Cyan),
    "repr.eui48" to Style(bold = true, color = Color.Cyan),

    // Rule
    "rule.line" to Style(color = Color.Green),
    "rule.text" to Style(),

    // JSON
    "json.brace" to Style(bold = true),
    "json.bool_true" to Style(color = Color.Green, italic = true),
    "json.bool_false" to Style(color = Color.Red, italic = true),
    "json.null" to Style(color = Color.Magenta, italic = true),
    "json.number" to Style(color = Color.Cyan, bold = true, italic = false),
    "json.str" to Style(color = Color.Green, italic = false, bold = false),
    "json.key" to Style(color = Color.Blue, bold = true),

    // Progress / table
    "bar.back" to Style(color = Color.Black),
    "bar.complete" to Style(color = Color.Red),
    "bar.finished" to Style(color = Color.Green),
    "bar.pulse" to Style(color = Color.Red),
    "progress.description" to Style.NULL,
    "progress.filesize" to Style(color = Color.Green),
    "progress.filesize.total" to Style(color = Color.Green),
    "progress.download" to Style(color = Color.Green),
    "progress.elapsed" to Style(color = Color.Yellow),
    "progress.percentage" to Style(color = Color.Magenta),
    "progress.remaining" to Style(color = Color.Cyan),
    "progress.data.speed" to Style(color = Color.Red),
    "progress.spinner" to Style(color = Color.Green),
    "status.spinner" to Style(color = Color.Green),
    "table.header" to Style(bold = true),
    "table.footer" to Style(bold = true),
    "table.cell" to Style.NULL,
    "table.title" to Style(italic = true),
    "table.caption" to Style(italic = true, dim = true),

    // Tree
    "tree" to Style.NULL,
    "tree.line" to Style.NULL,

    // Markdown
    "markdown.paragraph" to Style.NULL,
    "markdown.text" to Style.NULL,
    "markdown.em" to Style(italic = true),
    "markdown.strong" to Style(bold = true),
    "markdown.code" to Style(color = Color.Cyan, bgcolor = Color.Black),
    "markdown.code_block" to Style(color = Color.Cyan, bgcolor = Color.Black),
    "markdown.block_quote" to Style(color = Color.Magenta),
    "markdown.list" to Style(color = Color.Cyan),
    "markdown.item" to Style.NULL,
    "markdown.item.bullet" to Style(color = Color.Yellow, bold = true),
    "markdown.item.number" to Style(color = Color.Yellow, bold = true),
    "markdown.hr" to Style(color = Color.Yellow),
    "markdown.h1.border" to Style.NULL,
    "markdown.h1" to Style(bold = true),
    "markdown.h2" to Style(bold = true, underline = true),
    "markdown.h3" to Style(bold = true),
    "markdown.h4" to Style(bold = true, dim = true),
    "markdown.h5" to Style(underline = true),
    "markdown.h6" to Style(italic = true),
    "markdown.h7" to Style(italic = true, dim = true),
    "markdown.link" to Style(color = Color.Blue, underline = true),
    "markdown.link_url" to Style(color = Color.Blue, dim = true),

    // Traceback
    "traceback.error" to Style(color = Color.Red, italic = true),
    "traceback.border.syntax_error" to Style(color = Color.Red),
    "traceback.border" to Style(color = Color.Red),
    "traceback.text" to Style.NULL,
    "traceback.title" to Style(color = Color.Red, bold = true),
    "traceback.exc_type" to Style(color = Color.Red, bold = true),
    "traceback.exc_value" to Style.NULL,
    "traceback.offset" to Style(color = Color.Red, bold = true),
    "traceback.note" to Style(color = Color.Magenta, bold = true),

    // Inspect
    "inspect.attr" to Style(color = Color.Yellow, italic = true),
    "inspect.attr.dunder" to Style(color = Color.Yellow, italic = true, dim = true),
    "inspect.callable" to Style(bold = true, color = Color.Red),
    "inspect.async_def" to Style(italic = true, color = Color.Cyan),
    "inspect.def" to Style(italic = true, color = Color.Cyan),
    "inspect.class" to Style(italic = true, color = Color.Cyan),
    "inspect.error" to Style(bold = true, color = Color.Red),
    "inspect.equals" to Style.NULL,
    "inspect.help" to Style(color = Color.Cyan),
    "inspect.doc" to Style(dim = true),
    "inspect.value.border" to Style(color = Color.Green),

    // Prompt
    "prompt" to Style.NULL,
    "prompt.choices" to Style(color = Color.Magenta, bold = true),
    "prompt.default" to Style(color = Color.Cyan, bold = true),
    "prompt.invalid" to Style(color = Color.Red),
    "prompt.invalid.choice" to Style(color = Color.Red),
)
