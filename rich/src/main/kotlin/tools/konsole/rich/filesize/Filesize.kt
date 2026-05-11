package tools.konsole.rich.filesize

/** Format helpers for human-readable byte sizes. Mirrors `rich.filesize`. */
public object Filesize {

    /**
     * Decimal (SI) units: kB, MB, GB, …, using base 1000. Mirrors `rich.filesize.decimal`.
     *
     * @param bytes value to format.
     * @param precision digits after the decimal point. Defaults to 1 (matches rich).
     * @param separator string between value and suffix. Defaults to a single space (matches rich).
     */
    public fun decimal(bytes: Long, precision: Int = 1, separator: String = " "): String =
        format(bytes, 1000.0, DECIMAL_SUFFIXES, precision, separator)

    /**
     * Binary (IEC) units: KiB, MiB, GiB, …, using base 1024.
     * Konsole extension — Python rich's `filesize` module exposes only `decimal`,
     * but most JVM users expect binary too.
     */
    public fun binary(bytes: Long, precision: Int = 1, separator: String = " "): String =
        format(bytes, 1024.0, BINARY_SUFFIXES, precision, separator)

    /**
     * Pick the appropriate unit + numeric value for [size] given [suffixes] and [base].
     * Mirrors rich's `filesize.pick_unit_and_suffix` — used by progress columns
     * (TransferSpeedColumn, etc.) to share a single unit table.
     *
     * @return `Pair(unitFactor, suffix)` where `size / unitFactor` is the displayable value.
     */
    public fun pickUnitAndSuffix(size: Long, suffixes: List<String>, base: Int): Pair<Long, String> {
        if (size == 0L) return 1L to suffixes[0]
        var u = 0
        val absSize = if (size < 0) -size else size
        var v = absSize.toDouble()
        while (v >= base && u < suffixes.lastIndex) {
            v /= base
            u += 1
        }
        var factor = 1L
        repeat(u) { factor *= base }
        return factor to suffixes[u]
    }

    private val DECIMAL_SUFFIXES = arrayOf("B", "kB", "MB", "GB", "TB", "PB", "EB")
    private val BINARY_SUFFIXES = arrayOf("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB")

    private fun format(bytes: Long, base: Double, units: Array<String>, precision: Int, separator: String): String {
        if (bytes < 0) return "-${format(-bytes, base, units, precision, separator)}"
        var v = bytes.toDouble()
        var u = 0
        while (v >= base && u < units.lastIndex) {
            v /= base
            u += 1
        }
        return if (u == 0) "$bytes$separator${units[0]}"
        else "%.${precision}f%s%s".format(v, separator, units[u])
    }
}
