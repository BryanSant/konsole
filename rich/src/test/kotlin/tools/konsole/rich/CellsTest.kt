package tools.konsole.rich

import tools.konsole.core.Ansi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CellsTest : StringSpec({

    "ASCII is 1 cell per char" {
        Cells.cellLen("hello") shouldBe 5
        Cells.cellLen("") shouldBe 0
    }

    "CJK ideographs are 2 cells" {
        Cells.cellLen("漢字") shouldBe 4
        Cells.cellLen("中文") shouldBe 4
    }

    "Hiragana / Katakana are 2 cells" {
        Cells.cellLen("ひらがな") shouldBe 8
        Cells.cellLen("カタカナ") shouldBe 8
    }

    "fullwidth ASCII is 2 cells" {
        Cells.cellLen("Ａ") shouldBe 2
    }

    "emoji are 2 cells (surrogate pair)" {
        Cells.cellLen("🚀") shouldBe 2
        Cells.cellLen("😀😀") shouldBe 4
    }

    "control chars are 0 cells" {
        Cells.cellLen(Ansi.BEL) shouldBe 0
        Cells.cellLen("") shouldBe 0
    }

    "combining marks are 0 cells" {
        // 'a' + combining acute = 1 cell
        Cells.cellLen("á") shouldBe 1
    }

    "setCellSize pads short strings" {
        Cells.setCellSize("hi", 5) shouldBe "hi   "
    }

    "setCellSize truncates long strings" {
        Cells.setCellSize("hello world", 5) shouldBe "hello"
    }

    "setCellSize handles wide chars at boundary" {
        // "漢字x" is 5 cells; truncating to 3 should give "漢" + space (since 漢 is 2 cells, can't fit 字)
        Cells.setCellSize("漢字x", 3) shouldBe "漢 "
    }
})
