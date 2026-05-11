package tools.konsole.rich

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StripTest : StringSpec({

    "empty strip has zero cell length" {
        Strip.EMPTY.cellLength shouldBe 0
        Strip.EMPTY.isEmpty shouldBe true
    }

    "of() sums cell length across non-control segments" {
        val s = Strip.of(Segment("hello"), Segment(" "), Segment("world"))
        s.cellLength shouldBe 11
    }

    "wide chars count as 2 cells" {
        Strip.of(Segment("漢字")).cellLength shouldBe 4
    }

    "concatenation merges segments and adds lengths" {
        val a = Strip.of(Segment("foo"))
        val b = Strip.of(Segment("bar"))
        val ab = a + b
        ab.cellLength shouldBe 6
        ab.segments.size shouldBe 2
    }

    "adjustCellLength pads when target > current" {
        val s = Strip.of(Segment("x")).adjustCellLength(5)
        s.cellLength shouldBe 5
        s.segments.last().text shouldBe "    "
    }

    "adjustCellLength truncates when target < current" {
        val s = Strip.of(Segment("hello")).adjustCellLength(3)
        s.cellLength shouldBe 3
        s.segments.first().text shouldBe "hel"
    }

    "adjustCellLength leaves strip alone when equal" {
        val s = Strip.of(Segment("hello"))
        s.adjustCellLength(5) shouldBe s
    }
})
