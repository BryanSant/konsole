package tools.konsole.core.style

/**
 * Extension surface that grafts crossterm's `Stylize` trait onto Kotlin types.
 *
 * Every function returns a [StyledContent] you can compose by chaining further
 * style calls or print directly:
 *
 * ```
 * println("warn".yellow().bold().on(Color.Black))
 * println("rgb".with(Color.Rgb(120, 200, 80)).underlined())
 * ```
 *
 * Each call composes into the existing [ContentStyle] rather than overwriting,
 * so attributes accumulate while colors take the last-set value.
 */

// ---- Lifting plain types into StyledContent ----

public fun String.styled(): StyledContent<String> = StyledContent(ContentStyle.EMPTY, this)
public fun Char.styled(): StyledContent<Char> = StyledContent(ContentStyle.EMPTY, this)
public fun <T : Any> T.stylized(): StyledContent<T> = StyledContent(ContentStyle.EMPTY, this)

// ---- Generic style mutations ----

public fun <T : Any> T.with(color: Color): StyledContent<T> =
    StyledContent(ContentStyle(foreground = color), this)

public fun <T : Any> T.on(color: Color): StyledContent<T> =
    StyledContent(ContentStyle(background = color), this)

public fun <T : Any> T.underline(color: Color): StyledContent<T> =
    StyledContent(ContentStyle(underline = color), this)

public fun <T : Any> T.attribute(attr: Attribute): StyledContent<T> =
    StyledContent(ContentStyle(attributes = Attributes.of(attr)), this)

public fun <D> StyledContent<D>.with(color: Color): StyledContent<D> =
    copy(style = style.copy(foreground = color))

public fun <D> StyledContent<D>.on(color: Color): StyledContent<D> =
    copy(style = style.copy(background = color))

public fun <D> StyledContent<D>.underline(color: Color): StyledContent<D> =
    copy(style = style.copy(underline = color))

public fun <D> StyledContent<D>.attribute(attr: Attribute): StyledContent<D> =
    copy(style = style.copy(attributes = style.attributes + attr))

// ---- Foreground color shortcuts (String) ----

public fun String.black()        : StyledContent<String> = with(Color.Black)
public fun String.darkRed()      : StyledContent<String> = with(Color.DarkRed)
public fun String.darkGreen()    : StyledContent<String> = with(Color.DarkGreen)
public fun String.darkYellow()   : StyledContent<String> = with(Color.DarkYellow)
public fun String.darkBlue()     : StyledContent<String> = with(Color.DarkBlue)
public fun String.darkMagenta()  : StyledContent<String> = with(Color.DarkMagenta)
public fun String.darkCyan()     : StyledContent<String> = with(Color.DarkCyan)
public fun String.grey()         : StyledContent<String> = with(Color.Grey)
public fun String.darkGrey()     : StyledContent<String> = with(Color.DarkGrey)
public fun String.red()          : StyledContent<String> = with(Color.Red)
public fun String.green()        : StyledContent<String> = with(Color.Green)
public fun String.yellow()       : StyledContent<String> = with(Color.Yellow)
public fun String.blue()         : StyledContent<String> = with(Color.Blue)
public fun String.magenta()      : StyledContent<String> = with(Color.Magenta)
public fun String.cyan()         : StyledContent<String> = with(Color.Cyan)
public fun String.white()        : StyledContent<String> = with(Color.White)

// ---- Foreground shortcuts (StyledContent) ----

public fun <D> StyledContent<D>.black()       : StyledContent<D> = with(Color.Black)
public fun <D> StyledContent<D>.darkRed()     : StyledContent<D> = with(Color.DarkRed)
public fun <D> StyledContent<D>.darkGreen()   : StyledContent<D> = with(Color.DarkGreen)
public fun <D> StyledContent<D>.darkYellow()  : StyledContent<D> = with(Color.DarkYellow)
public fun <D> StyledContent<D>.darkBlue()    : StyledContent<D> = with(Color.DarkBlue)
public fun <D> StyledContent<D>.darkMagenta() : StyledContent<D> = with(Color.DarkMagenta)
public fun <D> StyledContent<D>.darkCyan()    : StyledContent<D> = with(Color.DarkCyan)
public fun <D> StyledContent<D>.grey()        : StyledContent<D> = with(Color.Grey)
public fun <D> StyledContent<D>.darkGrey()    : StyledContent<D> = with(Color.DarkGrey)
public fun <D> StyledContent<D>.red()         : StyledContent<D> = with(Color.Red)
public fun <D> StyledContent<D>.green()       : StyledContent<D> = with(Color.Green)
public fun <D> StyledContent<D>.yellow()      : StyledContent<D> = with(Color.Yellow)
public fun <D> StyledContent<D>.blue()        : StyledContent<D> = with(Color.Blue)
public fun <D> StyledContent<D>.magenta()     : StyledContent<D> = with(Color.Magenta)
public fun <D> StyledContent<D>.cyan()        : StyledContent<D> = with(Color.Cyan)
public fun <D> StyledContent<D>.white()       : StyledContent<D> = with(Color.White)

// ---- Background shortcuts (String) ----

public fun String.onBlack()       : StyledContent<String> = on(Color.Black)
public fun String.onDarkRed()     : StyledContent<String> = on(Color.DarkRed)
public fun String.onDarkGreen()   : StyledContent<String> = on(Color.DarkGreen)
public fun String.onDarkYellow()  : StyledContent<String> = on(Color.DarkYellow)
public fun String.onDarkBlue()    : StyledContent<String> = on(Color.DarkBlue)
public fun String.onDarkMagenta() : StyledContent<String> = on(Color.DarkMagenta)
public fun String.onDarkCyan()    : StyledContent<String> = on(Color.DarkCyan)
public fun String.onGrey()        : StyledContent<String> = on(Color.Grey)
public fun String.onDarkGrey()    : StyledContent<String> = on(Color.DarkGrey)
public fun String.onRed()         : StyledContent<String> = on(Color.Red)
public fun String.onGreen()       : StyledContent<String> = on(Color.Green)
public fun String.onYellow()      : StyledContent<String> = on(Color.Yellow)
public fun String.onBlue()        : StyledContent<String> = on(Color.Blue)
public fun String.onMagenta()     : StyledContent<String> = on(Color.Magenta)
public fun String.onCyan()        : StyledContent<String> = on(Color.Cyan)
public fun String.onWhite()       : StyledContent<String> = on(Color.White)

// ---- Background shortcuts (StyledContent) ----

public fun <D> StyledContent<D>.onBlack()       : StyledContent<D> = on(Color.Black)
public fun <D> StyledContent<D>.onDarkRed()     : StyledContent<D> = on(Color.DarkRed)
public fun <D> StyledContent<D>.onDarkGreen()   : StyledContent<D> = on(Color.DarkGreen)
public fun <D> StyledContent<D>.onDarkYellow()  : StyledContent<D> = on(Color.DarkYellow)
public fun <D> StyledContent<D>.onDarkBlue()    : StyledContent<D> = on(Color.DarkBlue)
public fun <D> StyledContent<D>.onDarkMagenta() : StyledContent<D> = on(Color.DarkMagenta)
public fun <D> StyledContent<D>.onDarkCyan()    : StyledContent<D> = on(Color.DarkCyan)
public fun <D> StyledContent<D>.onGrey()        : StyledContent<D> = on(Color.Grey)
public fun <D> StyledContent<D>.onDarkGrey()    : StyledContent<D> = on(Color.DarkGrey)
public fun <D> StyledContent<D>.onRed()         : StyledContent<D> = on(Color.Red)
public fun <D> StyledContent<D>.onGreen()       : StyledContent<D> = on(Color.Green)
public fun <D> StyledContent<D>.onYellow()      : StyledContent<D> = on(Color.Yellow)
public fun <D> StyledContent<D>.onBlue()        : StyledContent<D> = on(Color.Blue)
public fun <D> StyledContent<D>.onMagenta()     : StyledContent<D> = on(Color.Magenta)
public fun <D> StyledContent<D>.onCyan()        : StyledContent<D> = on(Color.Cyan)
public fun <D> StyledContent<D>.onWhite()       : StyledContent<D> = on(Color.White)

// ---- Attribute shortcuts (String) ----

public fun String.bold()             : StyledContent<String> = attribute(Attribute.Bold)
public fun String.dim()              : StyledContent<String> = attribute(Attribute.Dim)
public fun String.italic()           : StyledContent<String> = attribute(Attribute.Italic)
public fun String.underlined()       : StyledContent<String> = attribute(Attribute.Underlined)
public fun String.doubleUnderlined() : StyledContent<String> = attribute(Attribute.DoubleUnderlined)
public fun String.slowBlink()        : StyledContent<String> = attribute(Attribute.SlowBlink)
public fun String.rapidBlink()       : StyledContent<String> = attribute(Attribute.RapidBlink)
public fun String.reverse()          : StyledContent<String> = attribute(Attribute.Reverse)
public fun String.hidden()           : StyledContent<String> = attribute(Attribute.Hidden)
public fun String.crossedOut()       : StyledContent<String> = attribute(Attribute.CrossedOut)
public fun String.framed()           : StyledContent<String> = attribute(Attribute.Framed)
public fun String.encircled()        : StyledContent<String> = attribute(Attribute.Encircled)
public fun String.overLined()        : StyledContent<String> = attribute(Attribute.OverLined)

// ---- Attribute shortcuts (StyledContent) ----

public fun <D> StyledContent<D>.bold()             : StyledContent<D> = attribute(Attribute.Bold)
public fun <D> StyledContent<D>.dim()              : StyledContent<D> = attribute(Attribute.Dim)
public fun <D> StyledContent<D>.italic()           : StyledContent<D> = attribute(Attribute.Italic)
public fun <D> StyledContent<D>.underlined()       : StyledContent<D> = attribute(Attribute.Underlined)
public fun <D> StyledContent<D>.doubleUnderlined() : StyledContent<D> = attribute(Attribute.DoubleUnderlined)
public fun <D> StyledContent<D>.slowBlink()        : StyledContent<D> = attribute(Attribute.SlowBlink)
public fun <D> StyledContent<D>.rapidBlink()       : StyledContent<D> = attribute(Attribute.RapidBlink)
public fun <D> StyledContent<D>.reverse()          : StyledContent<D> = attribute(Attribute.Reverse)
public fun <D> StyledContent<D>.hidden()           : StyledContent<D> = attribute(Attribute.Hidden)
public fun <D> StyledContent<D>.crossedOut()       : StyledContent<D> = attribute(Attribute.CrossedOut)
public fun <D> StyledContent<D>.framed()           : StyledContent<D> = attribute(Attribute.Framed)
public fun <D> StyledContent<D>.encircled()        : StyledContent<D> = attribute(Attribute.Encircled)
public fun <D> StyledContent<D>.overLined()        : StyledContent<D> = attribute(Attribute.OverLined)
