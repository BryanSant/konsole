package tools.konsole.rich.emoji

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Validates that the [EmojiMap] catalogue matches Python rich's `_emoji_codes.py`.
 */
class EmojiTest : StringSpec({

    "EmojiMap contains exactly 3608 entries (matches rich)" {
        EmojiMap.size shouldBe 3608
    }

    "well-known shortcodes resolve to the expected unicode" {
        EmojiMap["rocket"] shouldBe "🚀"
        EmojiMap["thumbs_up"] shouldBe "👍"
        EmojiMap["heart"] shouldBe "❤"
        EmojiMap["smiley"] shouldBe "😃"
        EmojiMap["1st_place_medal"] shouldBe "🥇"
    }

    "unknown shortcodes return null" {
        EmojiMap["not_a_real_emoji_name_zzz"] shouldBe null
    }
})
