package tools.konsole.textual.compositor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip

class CellDiffTest : StringSpec({

    "identical strips emit zero bytes" {
        val a = Strip.of(Segment("hello world"))
        val b = Strip.of(Segment("hello world"))
        StripSerializer.serializeDiff(listOf(a), listOf(b)) shouldBe ""
    }

    "single changed character emits only that character + positioning" {
        val old = Strip.of(Segment("hello world"))
        val new = Strip.of(Segment("hello korld"))   // 1 cell differs at col 6 (w→k)
        val out = StripSerializer.serializeDiff(listOf(old), listOf(new))
        out shouldContain "k"
        out shouldNotContain "hello"   // unchanged prefix not re-emitted
        out shouldNotContain "world"   // unchanged suffix not re-emitted
    }

    "cell diff is dramatically smaller than full-row emission for sparse changes" {
        val wide = "x".repeat(80)
        val old = Strip.of(Segment(wide))
        val newText = "x".repeat(40) + "@" + "x".repeat(39)  // one cell differs
        val new = Strip.of(Segment(newText))
        val diffOut = StripSerializer.serializeDiff(listOf(old), listOf(new))
        val fullOut = StripSerializer.serialize(listOf(new))
        // Cell diff should be much smaller (~30 bytes) vs full emission (~90+ bytes).
        (diffOut.length < fullOut.length / 2) shouldBe true
    }

    "multiple separate changes get separate positioning sequences" {
        val old = Strip.of(Segment("aaaaaaaaaa"))   // 10 a's
        // Change positions 2 and 7
        val new = Strip.of(Segment("aaXaaaaYaa"))
        val out = StripSerializer.serializeDiff(listOf(old), listOf(new))
        out shouldContain "X"
        out shouldContain "Y"
        // Two separate positioning sequences (one for X, one for Y)
        val cursorMoves = out.split("H").size - 1
        (cursorMoves >= 2) shouldBe true
    }

    "consecutive changed cells are emitted as a single run with one positioning" {
        val old = Strip.of(Segment("aaaaaaa"))
        val new = Strip.of(Segment("aXXXXXa"))   // 5 contiguous changes in middle
        val out = StripSerializer.serializeDiff(listOf(old), listOf(new))
        val cursorMoves = out.split("H").size - 1
        cursorMoves shouldBe 1   // single positioning for the whole run
        out shouldContain "XXXXX"
    }

    "row that grew is emitted at the lengthened tail" {
        val old = Strip.of(Segment("short"))
        val new = Strip.of(Segment("short and longer"))
        val out = StripSerializer.serializeDiff(listOf(old), listOf(new))
        out shouldContain " and longer"
        out shouldNotContain "short"
    }
})
