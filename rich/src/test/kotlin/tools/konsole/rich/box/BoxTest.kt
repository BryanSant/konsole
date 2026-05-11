package tools.konsole.rich.box

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BoxTest : StringSpec({

    "ASCII top border" {
        // ASCII box uses '-' for both horizontal and divider, so '+' only at corners
        Box.ASCII.getTop(listOf(3, 5)) shouldBe "+---------+"
    }

    "SQUARE top border has divider between columns" {
        Box.SQUARE.getTop(listOf(3, 5)) shouldBe "┌───┬─────┐"
    }

    "ROUNDED corners" {
        Box.ROUNDED.topLeft shouldBe '╭'
        Box.ROUNDED.topRight shouldBe '╮'
        Box.ROUNDED.bottomLeft shouldBe '╰'
        Box.ROUNDED.bottomRight shouldBe '╯'
    }

    "HEAVY uses thick lines" {
        Box.HEAVY.topHorizontal shouldBe '━'
        Box.HEAVY.midLeft shouldBe '┃'
    }

    "DOUBLE uses double-line characters" {
        Box.DOUBLE.topHorizontal shouldBe '═'
        Box.DOUBLE.midLeft shouldBe '║'
    }

    "MARKDOWN matches GitHub-style table border" {
        // Header divider should be |-||
        Box.MARKDOWN.headRowLeft shouldBe '|'
        Box.MARKDOWN.headRowHorizontal shouldBe '-'
    }

    "ALL list contains all 19 box styles" {
        // 18 plus ASCII_DOUBLE_HEAD
        Box.ALL.size shouldBe 19
        Box.ALL.map { it.name }.toSet().size shouldBe 19
    }

    "asciiSafe flag set on ASCII variants" {
        Box.ASCII.asciiSafe shouldBe true
        Box.ASCII2.asciiSafe shouldBe true
        Box.MARKDOWN.asciiSafe shouldBe true
        Box.SQUARE.asciiSafe shouldBe false
        Box.ROUNDED.asciiSafe shouldBe false
    }

    "getRow draws full divider for multiple columns" {
        Box.SQUARE.getRow(listOf(2, 4, 1)) shouldBe "├──┼────┼─┤"
    }
})
