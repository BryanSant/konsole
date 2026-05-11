package tools.konsole.rich.spinner

/**
 * Catalogue of spinner animations, ported verbatim from Python rich's `_spinners.py`
 * (which itself is from npm cli-spinners by Sindre Sorhus, MIT-licensed).
 *
 * Each entry holds an [interval] (milliseconds between frames) and the [frames]
 * sequence. To consume programmatically use [SpinnerData.byName] or look up by
 * name on [SPINNERS].
 *
 * The 73 spinners here match the Python rich catalogue 1:1; their string keys
 * are the camelCase names used by Python so existing code that says
 * `Spinner("dots")` ports cleanly.
 */
public data class SpinnerData(
    public val name: String,
    public val interval: Int,
    public val frames: List<String>,
) {
    public companion object {
        /** Look up a spinner by its rich name. Throws if the name is unknown. */
        public fun byName(name: String): SpinnerData =
            SPINNERS[name] ?: error("unknown spinner name: $name (known: ${SPINNERS.keys.sorted().joinToString()})")
    }
}

/** All 73 spinners from rich, keyed by camelCase name. */
public val SPINNERS: Map<String, SpinnerData> = linkedMapOf(
    "aesthetic" to SpinnerData(
        name = "aesthetic",
        interval = 80,
        frames = listOf("▰▱▱▱▱▱▱", "▰▰▱▱▱▱▱", "▰▰▰▱▱▱▱", "▰▰▰▰▱▱▱", "▰▰▰▰▰▱▱", "▰▰▰▰▰▰▱", "▰▰▰▰▰▰▰", "▰▱▱▱▱▱▱"),
    ),
    "arc" to SpinnerData(
        name = "arc",
        interval = 100,
        frames = listOf("◜", "◠", "◝", "◞", "◡", "◟"),
    ),
    "arrow" to SpinnerData(
        name = "arrow",
        interval = 100,
        frames = listOf("←", "↖", "↑", "↗", "→", "↘", "↓", "↙"),
    ),
    "arrow2" to SpinnerData(
        name = "arrow2",
        interval = 80,
        frames = listOf("⬆️ ", "↗️ ", "➡️ ", "↘️ ", "⬇️ ", "↙️ ", "⬅️ ", "↖️ "),
    ),
    "arrow3" to SpinnerData(
        name = "arrow3",
        interval = 120,
        frames = listOf("▹▹▹▹▹", "▸▹▹▹▹", "▹▸▹▹▹", "▹▹▸▹▹", "▹▹▹▸▹", "▹▹▹▹▸"),
    ),
    "balloon" to SpinnerData(
        name = "balloon",
        interval = 140,
        frames = listOf(" ", ".", "o", "O", "@", "*", " "),
    ),
    "balloon2" to SpinnerData(
        name = "balloon2",
        interval = 120,
        frames = listOf(".", "o", "O", "°", "O", "o", "."),
    ),
    "betaWave" to SpinnerData(
        name = "betaWave",
        interval = 80,
        frames = listOf("ρββββββ", "βρβββββ", "ββρββββ", "βββρβββ", "ββββρββ", "βββββρβ", "ββββββρ"),
    ),
    "bounce" to SpinnerData(
        name = "bounce",
        interval = 120,
        frames = listOf("⠁", "⠂", "⠄", "⠂"),
    ),
    "bouncingBall" to SpinnerData(
        name = "bouncingBall",
        interval = 80,
        frames = listOf("( ●    )", "(  ●   )", "(   ●  )", "(    ● )", "(     ●)", "(    ● )", "(   ●  )", "(  ●   )", "( ●    )", "(●     )"),
    ),
    "bouncingBar" to SpinnerData(
        name = "bouncingBar",
        interval = 80,
        frames = listOf("[    ]", "[=   ]", "[==  ]", "[=== ]", "[ ===]", "[  ==]", "[   =]", "[    ]", "[   =]", "[  ==]", "[ ===]", "[====]", "[=== ]", "[==  ]", "[=   ]"),
    ),
    "boxBounce" to SpinnerData(
        name = "boxBounce",
        interval = 120,
        frames = listOf("▖", "▘", "▝", "▗"),
    ),
    "boxBounce2" to SpinnerData(
        name = "boxBounce2",
        interval = 100,
        frames = listOf("▌", "▀", "▐", "▄"),
    ),
    "christmas" to SpinnerData(
        name = "christmas",
        interval = 400,
        frames = listOf("🌲", "🎄"),
    ),
    "circle" to SpinnerData(
        name = "circle",
        interval = 120,
        frames = listOf("◡", "⊙", "◠"),
    ),
    "circleHalves" to SpinnerData(
        name = "circleHalves",
        interval = 50,
        frames = listOf("◐", "◓", "◑", "◒"),
    ),
    "circleQuarters" to SpinnerData(
        name = "circleQuarters",
        interval = 120,
        frames = listOf("◴", "◷", "◶", "◵"),
    ),
    "clock" to SpinnerData(
        name = "clock",
        interval = 100,
        frames = listOf("🕛 ", "🕐 ", "🕑 ", "🕒 ", "🕓 ", "🕔 ", "🕕 ", "🕖 ", "🕗 ", "🕘 ", "🕙 ", "🕚 "),
    ),
    "dots" to SpinnerData(
        name = "dots",
        interval = 80,
        frames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"),
    ),
    "dots10" to SpinnerData(
        name = "dots10",
        interval = 80,
        frames = listOf("⢄", "⢂", "⢁", "⡁", "⡈", "⡐", "⡠"),
    ),
    "dots11" to SpinnerData(
        name = "dots11",
        interval = 100,
        frames = listOf("⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈"),
    ),
    "dots12" to SpinnerData(
        name = "dots12",
        interval = 80,
        frames = listOf("⢀⠀", "⡀⠀", "⠄⠀", "⢂⠀", "⡂⠀", "⠅⠀", "⢃⠀", "⡃⠀", "⠍⠀", "⢋⠀", "⡋⠀", "⠍⠁", "⢋⠁", "⡋⠁", "⠍⠉", "⠋⠉", "⠋⠉", "⠉⠙", "⠉⠙", "⠉⠩", "⠈⢙", "⠈⡙", "⢈⠩", "⡀⢙", "⠄⡙", "⢂⠩", "⡂⢘", "⠅⡘", "⢃⠨", "⡃⢐", "⠍⡐", "⢋⠠", "⡋⢀", "⠍⡁", "⢋⠁", "⡋⠁", "⠍⠉", "⠋⠉", "⠋⠉", "⠉⠙", "⠉⠙", "⠉⠩", "⠈⢙", "⠈⡙", "⠈⠩", "⠀⢙", "⠀⡙", "⠀⠩", "⠀⢘", "⠀⡘", "⠀⠨", "⠀⢐", "⠀⡐", "⠀⠠", "⠀⢀", "⠀⡀"),
    ),
    "dots2" to SpinnerData(
        name = "dots2",
        interval = 80,
        frames = listOf("⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"),
    ),
    "dots3" to SpinnerData(
        name = "dots3",
        interval = 80,
        frames = listOf("⠋", "⠙", "⠚", "⠞", "⠖", "⠦", "⠴", "⠲", "⠳", "⠓"),
    ),
    "dots4" to SpinnerData(
        name = "dots4",
        interval = 80,
        frames = listOf("⠄", "⠆", "⠇", "⠋", "⠙", "⠸", "⠰", "⠠", "⠰", "⠸", "⠙", "⠋", "⠇", "⠆"),
    ),
    "dots5" to SpinnerData(
        name = "dots5",
        interval = 80,
        frames = listOf("⠋", "⠙", "⠚", "⠒", "⠂", "⠂", "⠒", "⠲", "⠴", "⠦", "⠖", "⠒", "⠐", "⠐", "⠒", "⠓", "⠋"),
    ),
    "dots6" to SpinnerData(
        name = "dots6",
        interval = 80,
        frames = listOf("⠁", "⠉", "⠙", "⠚", "⠒", "⠂", "⠂", "⠒", "⠲", "⠴", "⠤", "⠄", "⠄", "⠤", "⠴", "⠲", "⠒", "⠂", "⠂", "⠒", "⠚", "⠙", "⠉", "⠁"),
    ),
    "dots7" to SpinnerData(
        name = "dots7",
        interval = 80,
        frames = listOf("⠈", "⠉", "⠋", "⠓", "⠒", "⠐", "⠐", "⠒", "⠖", "⠦", "⠤", "⠠", "⠠", "⠤", "⠦", "⠖", "⠒", "⠐", "⠐", "⠒", "⠓", "⠋", "⠉", "⠈"),
    ),
    "dots8" to SpinnerData(
        name = "dots8",
        interval = 80,
        frames = listOf("⠁", "⠁", "⠉", "⠙", "⠚", "⠒", "⠂", "⠂", "⠒", "⠲", "⠴", "⠤", "⠄", "⠄", "⠤", "⠠", "⠠", "⠤", "⠦", "⠖", "⠒", "⠐", "⠐", "⠒", "⠓", "⠋", "⠉", "⠈", "⠈"),
    ),
    "dots8Bit" to SpinnerData(
        name = "dots8Bit",
        interval = 80,
        frames = listOf("⠀", "⠁", "⠂", "⠃", "⠄", "⠅", "⠆", "⠇", "⡀", "⡁", "⡂", "⡃", "⡄", "⡅", "⡆", "⡇", "⠈", "⠉", "⠊", "⠋", "⠌", "⠍", "⠎", "⠏", "⡈", "⡉", "⡊", "⡋", "⡌", "⡍", "⡎", "⡏", "⠐", "⠑", "⠒", "⠓", "⠔", "⠕", "⠖", "⠗", "⡐", "⡑", "⡒", "⡓", "⡔", "⡕", "⡖", "⡗", "⠘", "⠙", "⠚", "⠛", "⠜", "⠝", "⠞", "⠟", "⡘", "⡙", "⡚", "⡛", "⡜", "⡝", "⡞", "⡟", "⠠", "⠡", "⠢", "⠣", "⠤", "⠥", "⠦", "⠧", "⡠", "⡡", "⡢", "⡣", "⡤", "⡥", "⡦", "⡧", "⠨", "⠩", "⠪", "⠫", "⠬", "⠭", "⠮", "⠯", "⡨", "⡩", "⡪", "⡫", "⡬", "⡭", "⡮", "⡯", "⠰", "⠱", "⠲", "⠳", "⠴", "⠵", "⠶", "⠷", "⡰", "⡱", "⡲", "⡳", "⡴", "⡵", "⡶", "⡷", "⠸", "⠹", "⠺", "⠻", "⠼", "⠽", "⠾", "⠿", "⡸", "⡹", "⡺", "⡻", "⡼", "⡽", "⡾", "⡿", "⢀", "⢁", "⢂", "⢃", "⢄", "⢅", "⢆", "⢇", "⣀", "⣁", "⣂", "⣃", "⣄", "⣅", "⣆", "⣇", "⢈", "⢉", "⢊", "⢋", "⢌", "⢍", "⢎", "⢏", "⣈", "⣉", "⣊", "⣋", "⣌", "⣍", "⣎", "⣏", "⢐", "⢑", "⢒", "⢓", "⢔", "⢕", "⢖", "⢗", "⣐", "⣑", "⣒", "⣓", "⣔", "⣕", "⣖", "⣗", "⢘", "⢙", "⢚", "⢛", "⢜", "⢝", "⢞", "⢟", "⣘", "⣙", "⣚", "⣛", "⣜", "⣝", "⣞", "⣟", "⢠", "⢡", "⢢", "⢣", "⢤", "⢥", "⢦", "⢧", "⣠", "⣡", "⣢", "⣣", "⣤", "⣥", "⣦", "⣧", "⢨", "⢩", "⢪", "⢫", "⢬", "⢭", "⢮", "⢯", "⣨", "⣩", "⣪", "⣫", "⣬", "⣭", "⣮", "⣯", "⢰", "⢱", "⢲", "⢳", "⢴", "⢵", "⢶", "⢷", "⣰", "⣱", "⣲", "⣳", "⣴", "⣵", "⣶", "⣷", "⢸", "⢹", "⢺", "⢻", "⢼", "⢽", "⢾", "⢿", "⣸", "⣹", "⣺", "⣻", "⣼", "⣽", "⣾", "⣿"),
    ),
    "dots9" to SpinnerData(
        name = "dots9",
        interval = 80,
        frames = listOf("⢹", "⢺", "⢼", "⣸", "⣇", "⡧", "⡗", "⡏"),
    ),
    "dqpb" to SpinnerData(
        name = "dqpb",
        interval = 100,
        frames = listOf("d", "q", "p", "b"),
    ),
    "earth" to SpinnerData(
        name = "earth",
        interval = 180,
        frames = listOf("🌍 ", "🌎 ", "🌏 "),
    ),
    "flip" to SpinnerData(
        name = "flip",
        interval = 70,
        frames = listOf("_", "_", "_", "-", "`", "`", "'", "´", "-", "_", "_", "_"),
    ),
    "grenade" to SpinnerData(
        name = "grenade",
        interval = 80,
        frames = listOf("،   ", "′   ", " ´ ", " ‾ ", "  ⸌", "  ⸊", "  |", "  ⁎", "  ⁕", " ෴ ", "  ⁓", "   ", "   ", "   "),
    ),
    "growHorizontal" to SpinnerData(
        name = "growHorizontal",
        interval = 120,
        frames = listOf("▏", "▎", "▍", "▌", "▋", "▊", "▉", "▊", "▋", "▌", "▍", "▎"),
    ),
    "growVertical" to SpinnerData(
        name = "growVertical",
        interval = 120,
        frames = listOf("▁", "▃", "▄", "▅", "▆", "▇", "▆", "▅", "▄", "▃"),
    ),
    "hamburger" to SpinnerData(
        name = "hamburger",
        interval = 100,
        frames = listOf("☱", "☲", "☴"),
    ),
    "hearts" to SpinnerData(
        name = "hearts",
        interval = 100,
        frames = listOf("💛 ", "💙 ", "💜 ", "💚 ", "❤️ "),
    ),
    "layer" to SpinnerData(
        name = "layer",
        interval = 150,
        frames = listOf("-", "=", "≡"),
    ),
    "line" to SpinnerData(
        name = "line",
        interval = 130,
        frames = listOf("-", "\\", "|", "/"),
    ),
    "line2" to SpinnerData(
        name = "line2",
        interval = 100,
        frames = listOf("⠂", "-", "–", "—", "–", "-"),
    ),
    "material" to SpinnerData(
        name = "material",
        interval = 17,
        frames = listOf("█▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "██▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "███▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "████▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "██████▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "██████▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "███████▁▁▁▁▁▁▁▁▁▁▁▁▁", "████████▁▁▁▁▁▁▁▁▁▁▁▁", "█████████▁▁▁▁▁▁▁▁▁▁▁", "█████████▁▁▁▁▁▁▁▁▁▁▁", "██████████▁▁▁▁▁▁▁▁▁▁", "███████████▁▁▁▁▁▁▁▁▁", "█████████████▁▁▁▁▁▁▁", "██████████████▁▁▁▁▁▁", "██████████████▁▁▁▁▁▁", "▁██████████████▁▁▁▁▁", "▁██████████████▁▁▁▁▁", "▁██████████████▁▁▁▁▁", "▁▁██████████████▁▁▁▁", "▁▁▁██████████████▁▁▁", "▁▁▁▁█████████████▁▁▁", "▁▁▁▁██████████████▁▁", "▁▁▁▁██████████████▁▁", "▁▁▁▁▁██████████████▁", "▁▁▁▁▁██████████████▁", "▁▁▁▁▁██████████████▁", "▁▁▁▁▁▁██████████████", "▁▁▁▁▁▁██████████████", "▁▁▁▁▁▁▁█████████████", "▁▁▁▁▁▁▁█████████████", "▁▁▁▁▁▁▁▁████████████", "▁▁▁▁▁▁▁▁████████████", "▁▁▁▁▁▁▁▁▁███████████", "▁▁▁▁▁▁▁▁▁███████████", "▁▁▁▁▁▁▁▁▁▁██████████", "▁▁▁▁▁▁▁▁▁▁██████████", "▁▁▁▁▁▁▁▁▁▁▁▁████████", "▁▁▁▁▁▁▁▁▁▁▁▁▁███████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁██████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁█████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁█████", "█▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁████", "██▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁███", "██▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁███", "███▁▁▁▁▁▁▁▁▁▁▁▁▁▁███", "████▁▁▁▁▁▁▁▁▁▁▁▁▁▁██", "█████▁▁▁▁▁▁▁▁▁▁▁▁▁▁█", "█████▁▁▁▁▁▁▁▁▁▁▁▁▁▁█", "██████▁▁▁▁▁▁▁▁▁▁▁▁▁█", "████████▁▁▁▁▁▁▁▁▁▁▁▁", "█████████▁▁▁▁▁▁▁▁▁▁▁", "█████████▁▁▁▁▁▁▁▁▁▁▁", "█████████▁▁▁▁▁▁▁▁▁▁▁", "█████████▁▁▁▁▁▁▁▁▁▁▁", "███████████▁▁▁▁▁▁▁▁▁", "████████████▁▁▁▁▁▁▁▁", "████████████▁▁▁▁▁▁▁▁", "██████████████▁▁▁▁▁▁", "██████████████▁▁▁▁▁▁", "▁██████████████▁▁▁▁▁", "▁██████████████▁▁▁▁▁", "▁▁▁█████████████▁▁▁▁", "▁▁▁▁▁████████████▁▁▁", "▁▁▁▁▁████████████▁▁▁", "▁▁▁▁▁▁███████████▁▁▁", "▁▁▁▁▁▁▁▁█████████▁▁▁", "▁▁▁▁▁▁▁▁█████████▁▁▁", "▁▁▁▁▁▁▁▁▁█████████▁▁", "▁▁▁▁▁▁▁▁▁█████████▁▁", "▁▁▁▁▁▁▁▁▁▁█████████▁", "▁▁▁▁▁▁▁▁▁▁▁████████▁", "▁▁▁▁▁▁▁▁▁▁▁████████▁", "▁▁▁▁▁▁▁▁▁▁▁▁███████▁", "▁▁▁▁▁▁▁▁▁▁▁▁███████▁", "▁▁▁▁▁▁▁▁▁▁▁▁▁███████", "▁▁▁▁▁▁▁▁▁▁▁▁▁███████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁█████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁████", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁███", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁███", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁██", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁██", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁██", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁█", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁█", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁█", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁"),
    ),
    "monkey" to SpinnerData(
        name = "monkey",
        interval = 300,
        frames = listOf("🙈 ", "🙈 ", "🙉 ", "🙊 "),
    ),
    "moon" to SpinnerData(
        name = "moon",
        interval = 80,
        frames = listOf("🌑 ", "🌒 ", "🌓 ", "🌔 ", "🌕 ", "🌖 ", "🌗 ", "🌘 "),
    ),
    "noise" to SpinnerData(
        name = "noise",
        interval = 100,
        frames = listOf("▓", "▒", "░"),
    ),
    "pipe" to SpinnerData(
        name = "pipe",
        interval = 100,
        frames = listOf("┤", "┘", "┴", "└", "├", "┌", "┬", "┐"),
    ),
    "point" to SpinnerData(
        name = "point",
        interval = 125,
        frames = listOf("∙∙∙", "●∙∙", "∙●∙", "∙∙●", "∙∙∙"),
    ),
    "pong" to SpinnerData(
        name = "pong",
        interval = 80,
        frames = listOf("▐⠂       ▌", "▐⠈       ▌", "▐ ⠂      ▌", "▐ ⠠      ▌", "▐  ⡀     ▌", "▐  ⠠     ▌", "▐   ⠂    ▌", "▐   ⠈    ▌", "▐    ⠂   ▌", "▐    ⠠   ▌", "▐     ⡀  ▌", "▐     ⠠  ▌", "▐      ⠂ ▌", "▐      ⠈ ▌", "▐       ⠂▌", "▐       ⠠▌", "▐       ⡀▌", "▐      ⠠ ▌", "▐      ⠂ ▌", "▐     ⠈  ▌", "▐     ⠂  ▌", "▐    ⠠   ▌", "▐    ⡀   ▌", "▐   ⠠    ▌", "▐   ⠂    ▌", "▐  ⠈     ▌", "▐  ⠂     ▌", "▐ ⠠      ▌", "▐ ⡀      ▌", "▐⠠       ▌"),
    ),
    "runner" to SpinnerData(
        name = "runner",
        interval = 140,
        frames = listOf("🚶 ", "🏃 "),
    ),
    "shark" to SpinnerData(
        name = "shark",
        interval = 120,
        frames = listOf("▐|\\____________▌", "▐_|\\___________▌", "▐__|\\__________▌", "▐___|\\_________▌", "▐____|\\________▌", "▐_____|\\_______▌", "▐______|\\______▌", "▐_______|\\_____▌", "▐________|\\____▌", "▐_________|\\___▌", "▐__________|\\__▌", "▐___________|\\_▌", "▐____________|\\▌", "▐____________/|▌", "▐___________/|_▌", "▐__________/|__▌", "▐_________/|___▌", "▐________/|____▌", "▐_______/|_____▌", "▐______/|______▌", "▐_____/|_______▌", "▐____/|________▌", "▐___/|_________▌", "▐__/|__________▌", "▐_/|___________▌", "▐/|____________▌"),
    ),
    "simpleDots" to SpinnerData(
        name = "simpleDots",
        interval = 400,
        frames = listOf(".  ", ".. ", "...", "   "),
    ),
    "simpleDotsScrolling" to SpinnerData(
        name = "simpleDotsScrolling",
        interval = 200,
        frames = listOf(".  ", ".. ", "...", " ..", "  .", "   "),
    ),
    "smiley" to SpinnerData(
        name = "smiley",
        interval = 200,
        frames = listOf("😄 ", "😝 "),
    ),
    "squareCorners" to SpinnerData(
        name = "squareCorners",
        interval = 180,
        frames = listOf("◰", "◳", "◲", "◱"),
    ),
    "squish" to SpinnerData(
        name = "squish",
        interval = 100,
        frames = listOf("╫", "╪"),
    ),
    "star" to SpinnerData(
        name = "star",
        interval = 70,
        frames = listOf("✶", "✸", "✹", "✺", "✹", "✷"),
    ),
    "star2" to SpinnerData(
        name = "star2",
        interval = 80,
        frames = listOf("+", "x", "*"),
    ),
    "toggle" to SpinnerData(
        name = "toggle",
        interval = 250,
        frames = listOf("⊶", "⊷"),
    ),
    "toggle10" to SpinnerData(
        name = "toggle10",
        interval = 100,
        frames = listOf("㊂", "㊀", "㊁"),
    ),
    "toggle11" to SpinnerData(
        name = "toggle11",
        interval = 50,
        frames = listOf("⧇", "⧆"),
    ),
    "toggle12" to SpinnerData(
        name = "toggle12",
        interval = 120,
        frames = listOf("☗", "☖"),
    ),
    "toggle13" to SpinnerData(
        name = "toggle13",
        interval = 80,
        frames = listOf("=", "*", "-"),
    ),
    "toggle2" to SpinnerData(
        name = "toggle2",
        interval = 80,
        frames = listOf("▫", "▪"),
    ),
    "toggle3" to SpinnerData(
        name = "toggle3",
        interval = 120,
        frames = listOf("□", "■"),
    ),
    "toggle4" to SpinnerData(
        name = "toggle4",
        interval = 100,
        frames = listOf("■", "□", "▪", "▫"),
    ),
    "toggle5" to SpinnerData(
        name = "toggle5",
        interval = 100,
        frames = listOf("▮", "▯"),
    ),
    "toggle6" to SpinnerData(
        name = "toggle6",
        interval = 300,
        frames = listOf("ဝ", "၀"),
    ),
    "toggle7" to SpinnerData(
        name = "toggle7",
        interval = 80,
        frames = listOf("⦾", "⦿"),
    ),
    "toggle8" to SpinnerData(
        name = "toggle8",
        interval = 100,
        frames = listOf("◍", "◌"),
    ),
    "toggle9" to SpinnerData(
        name = "toggle9",
        interval = 100,
        frames = listOf("◉", "◎"),
    ),
    "triangle" to SpinnerData(
        name = "triangle",
        interval = 50,
        frames = listOf("◢", "◣", "◤", "◥"),
    ),
    "weather" to SpinnerData(
        name = "weather",
        interval = 100,
        frames = listOf("☀️ ", "☀️ ", "☀️ ", "🌤 ", "⛅️ ", "🌥 ", "☁️ ", "🌧 ", "🌨 ", "🌧 ", "🌨 ", "🌧 ", "🌨 ", "⛈ ", "🌨 ", "🌧 ", "🌨 ", "☁️ ", "🌥 ", "⛅️ ", "🌤 ", "☀️ ", "☀️ "),
    ),
)
