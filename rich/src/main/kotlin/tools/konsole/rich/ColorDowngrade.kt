package tools.konsole.rich

import tools.konsole.core.ColorSystem
import tools.konsole.core.style.Color

/**
 * Downgrade [color] to whatever this terminal can emit. Truecolor pass-through;
 * [ColorSystem.EightBit] folds RGB to the 256-color palette; [ColorSystem.Standard]
 * and [ColorSystem.Windows] fold both RGB and 256-palette to the 16 named colors;
 * [ColorSystem.None] strips color entirely.
 */
public fun ColorSystem.downgrade(color: Color): Color = when (this) {
    ColorSystem.None -> Color.Reset
    ColorSystem.TrueColor -> color
    ColorSystem.EightBit -> when (color) {
        is Color.Rgb -> color.toAnsi256()
        else -> color
    }
    ColorSystem.Standard, ColorSystem.Windows -> when (color) {
        is Color.Rgb -> color.toNamed16()
        is Color.AnsiValue -> color.toNamed16()
        else -> color
    }
}

/**
 * Downgrade a 24-bit RGB color to the closest 256-palette index using the standard
 * xterm 6×6×6 cube + 24-step grayscale ramp.
 */
public fun Color.Rgb.toAnsi256(): Color.AnsiValue {
    val r = this.r
    val g = this.g
    val b = this.b
    // Grayscale fast path: r==g==b within tolerance maps to the 24-step ramp 232..255.
    if (kotlin.math.abs(r - g) <= 4 && kotlin.math.abs(g - b) <= 4) {
        val gray = (r + g + b) / 3
        // 8 → 232, 18 → 233, …, 238 → 255. Linear: index = round((v - 8) / 10), clamped.
        val idx = if (gray < 8) 16 else if (gray > 238) 231 else 232 + ((gray - 8) / 10).coerceIn(0, 23)
        return Color.AnsiValue(idx)
    }
    val ri = quantize6(r)
    val gi = quantize6(g)
    val bi = quantize6(b)
    return Color.AnsiValue(16 + 36 * ri + 6 * gi + bi)
}

private fun quantize6(component: Int): Int {
    // xterm's 6 levels: 0, 95, 135, 175, 215, 255
    val levels = intArrayOf(0, 95, 135, 175, 215, 255)
    var bestIdx = 0
    var bestDist = Int.MAX_VALUE
    for (i in levels.indices) {
        val d = kotlin.math.abs(component - levels[i])
        if (d < bestDist) {
            bestDist = d
            bestIdx = i
        }
    }
    return bestIdx
}

/** Downgrade an RGB color to the closest of 16 named colors by Euclidean distance. */
public fun Color.Rgb.toNamed16(): Color = nearestNamed16(r, g, b)

/** Downgrade a 256-palette index to the closest of 16 named colors. */
public fun Color.AnsiValue.toNamed16(): Color {
    if (this.value < 16) return ANSI16_BY_INDEX[this.value]
    val (r, g, b) = ansi256Rgb(this.value)
    return nearestNamed16(r, g, b)
}

private fun nearestNamed16(r: Int, g: Int, b: Int): Color {
    var best: Color = Color.Reset
    var bestDist = Int.MAX_VALUE
    for (i in NAMED_16_RGB.indices) {
        val (nr, ng, nb) = NAMED_16_RGB[i]
        val dr = r - nr
        val dg = g - ng
        val db = b - nb
        val d = dr * dr + dg * dg + db * db
        if (d < bestDist) {
            bestDist = d
            best = ANSI16_BY_INDEX[i]
        }
    }
    return best
}

/** Map an xterm 256-palette index back to RGB triplet. */
internal fun ansi256Rgb(index: Int): Triple<Int, Int, Int> = when {
    index < 16 -> {
        val (r, g, b) = NAMED_16_RGB[index]
        Triple(r, g, b)
    }
    index in 16..231 -> {
        val n = index - 16
        val r = (n / 36) % 6
        val g = (n / 6) % 6
        val b = n % 6
        val levels = intArrayOf(0, 95, 135, 175, 215, 255)
        Triple(levels[r], levels[g], levels[b])
    }
    else -> {
        // 232..255 grayscale: 8, 18, ..., 238
        val v = 8 + (index - 232) * 10
        Triple(v, v, v)
    }
}

/** xterm-style RGB triplets for the 16 named colors (in ANSI index order 0..15). */
private val NAMED_16_RGB: Array<Triple<Int, Int, Int>> = arrayOf(
    Triple(0, 0, 0),         // 0  Black
    Triple(128, 0, 0),       // 1  DarkRed
    Triple(0, 128, 0),       // 2  DarkGreen
    Triple(128, 128, 0),     // 3  DarkYellow
    Triple(0, 0, 128),       // 4  DarkBlue
    Triple(128, 0, 128),     // 5  DarkMagenta
    Triple(0, 128, 128),     // 6  DarkCyan
    Triple(192, 192, 192),   // 7  Grey
    Triple(128, 128, 128),   // 8  DarkGrey
    Triple(255, 0, 0),       // 9  Red
    Triple(0, 255, 0),       // 10 Green
    Triple(255, 255, 0),     // 11 Yellow
    Triple(0, 0, 255),       // 12 Blue
    Triple(255, 0, 255),     // 13 Magenta
    Triple(0, 255, 255),     // 14 Cyan
    Triple(255, 255, 255),   // 15 White
)

private val ANSI16_BY_INDEX: Array<Color> = arrayOf(
    Color.Black,
    Color.DarkRed,
    Color.DarkGreen,
    Color.DarkYellow,
    Color.DarkBlue,
    Color.DarkMagenta,
    Color.DarkCyan,
    Color.Grey,
    Color.DarkGrey,
    Color.Red,
    Color.Green,
    Color.Yellow,
    Color.Blue,
    Color.Magenta,
    Color.Cyan,
    Color.White,
)
