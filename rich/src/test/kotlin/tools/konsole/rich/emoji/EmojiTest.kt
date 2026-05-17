package tools.konsole.rich.emoji

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EmojiTest : StringSpec({

    "EmojiMap contains exactly 50 entries" {
        EmojiMap.size shouldBe 50
    }

    "well-known shortcodes resolve to the expected unicode" {
        EmojiMap["rocket"] shouldBe "🚀"
        EmojiMap["+1"] shouldBe "👍"
        EmojiMap["heart"] shouldBe "❤"
        EmojiMap["smiley"] shouldBe "😃"
        EmojiMap["tada"] shouldBe "🎉"
    }

    "unknown shortcodes return null" {
        EmojiMap["not_a_real_emoji_name_zzz"] shouldBe null
    }

    "shortcodes dropped from the rich catalogue now return null" {
        EmojiMap["thumbs_up"] shouldBe null
        EmojiMap["1st_place_medal"] shouldBe null
    }
})
