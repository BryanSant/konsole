package tools.konsole.rich.emoji

/**
 * Emoji shortcode → Unicode mapping for the 50 most-commonly-used shortcodes.
 *
 * Used by [tools.konsole.rich.markup.Markup] to expand `:shortcode:` syntax.
 * Unknown shortcodes pass through as the literal `:name:` text.
 */
public object EmojiMap {

    public val EMOJI: Map<String, String> = mapOf(
        "+1" to "👍",
        "100" to "💯",
        "art" to "🎨",
        "blush" to "😊",
        "books" to "📚",
        "bug" to "🐛",
        "bulb" to "💡",
        "clap" to "👏",
        "cry" to "😢",
        "eyes" to "👀",
        "fearful" to "😨",
        "fire" to "🔥",
        "flushed" to "😳",
        "gem" to "💎",
        "grin" to "😁",
        "hammer" to "🔨",
        "heart" to "❤",
        "heart_eyes" to "😍",
        "joy" to "😂",
        "kissing_heart" to "😘",
        "lock" to "🔒",
        "memo" to "📝",
        "muscle" to "💪",
        "ok_hand" to "👌",
        "point_right" to "👉",
        "pray" to "🙏",
        "purple_heart" to "💜",
        "rage" to "😡",
        "raised_hands" to "🙌",
        "relieved" to "😌",
        "rocket" to "🚀",
        "rolling_on_the_floor_laughing" to "🤣",
        "skull" to "💀",
        "smile" to "😄",
        "smiley" to "😃",
        "smirk" to "😏",
        "sob" to "😭",
        "sparkles" to "✨",
        "star" to "⭐",
        "sunglasses" to "😎",
        "tada" to "🎉",
        "thinking_face" to "🤔",
        "two_hearts" to "💕",
        "warning" to "⚠",
        "wave" to "👋",
        "weary" to "😩",
        "white_check_mark" to "✅",
        "wink" to "😉",
        "wrench" to "🔧",
        "zap" to "⚡",
    )

    /** Look up an emoji by shortcode, returning `null` if not present. */
    public operator fun get(name: String): String? = EMOJI[name]

    /** Number of shortcodes available. */
    public val size: Int get() = EMOJI.size
}
